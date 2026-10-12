package sn.jappo.jappo_backend.ia.context;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;
import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.entity.Ressource;
import sn.jappo.jappo_backend.ressource.repository.RessourceRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AiResourceContextService {

    private static final int MAX_RESOURCES = 40;
    private static final int MAX_TEXT_PER_RESOURCE = 6_000;
    private static final int MAX_TOTAL_TEXT = 240_000;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_DOWNLOAD_FILE_SIZE = 50L * 1024 * 1024;
    private static final UUID NO_MISSION_RESOURCE = new UUID(0, 0);
    private static final Set<String> PARSEABLE_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final Set<String> DOWNLOADABLE_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                    "png", "jpg", "jpeg", "gif", "webp", "zip", "mp4", "mov", "avi");

    private final RessourceRepository ressourceRepository;
    private final Path uploadBase;

    public record ResourceFile(String filename, String contentType, byte[] content) {}

    public AiResourceContextService(
            RessourceRepository ressourceRepository,
            @Value("${app.upload.base-dir:uploads}") String uploadBase) {
        this.ressourceRepository = ressourceRepository;
        this.uploadBase = Paths.get(uploadBase).toAbsolutePath().normalize();
    }

    public List<AiContext.SourceContext> loadForEntrepreneur(
            UUID structureId,
            UUID cohorteId,
            UUID parcoursId,
            UUID phaseId,
            List<UUID> missionResourceIds) {
        List<UUID> ids = missionResourceIds == null || missionResourceIds.isEmpty()
                ? List.of(NO_MISSION_RESOURCE)
                : missionResourceIds;
        List<Ressource> resources = ressourceRepository.findAccessiblesEntrepreneur(
                structureId,
                singletonOrPlaceholder(cohorteId),
                singletonOrPlaceholder(parcoursId),
                singletonOrPlaceholder(phaseId),
                ids);
        resources.sort(Comparator
                .comparingInt((Ressource resource) -> scopePriority(resource.getPortee()))
                .thenComparing(Ressource::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<AiContext.SourceContext> result = new ArrayList<>();
        int totalText = 0;
        for (Ressource resource : resources) {
            if (result.size() == MAX_RESOURCES || totalText >= MAX_TOTAL_TEXT) {
                break;
            }
            String text = extractText(resource, structureId);
            if (text.isBlank()) {
                text = resource.getDescription() == null ? "" : resource.getDescription().trim();
            }
            int remaining = MAX_TOTAL_TEXT - totalText;
            if (text.length() > Math.min(MAX_TEXT_PER_RESOURCE, remaining)) {
                text = text.substring(0, Math.min(MAX_TEXT_PER_RESOURCE, remaining));
            }
            totalText += text.length();
            String title = resource.getTitre() == null ? "Ressource" : resource.getTitre();
            String snippet = text.length() > 400 ? text.substring(0, 397) + "..." : text;
            String url = resource.getUrl() == null || resource.getUrl().isBlank()
                    ? "/api/ressources/" + resource.getId()
                    : resource.getUrl();
            result.add(new AiContext.SourceContext(resource.getId(), title, url, snippet, text));
        }
        return result;
    }

    public ResourceFile loadStoredResourceFile(UUID structureId, UUID resourceId) {
        return loadStoredResourceFile(structureId, resourceId, PARSEABLE_EXTENSIONS, MAX_FILE_SIZE);
    }

    public ResourceFile loadAccessibleResourceFile(UUID structureId, UUID resourceId) {
        return loadStoredResourceFile(
                structureId, resourceId, DOWNLOADABLE_EXTENSIONS, MAX_DOWNLOAD_FILE_SIZE);
    }

    private ResourceFile loadStoredResourceFile(
            UUID structureId, UUID resourceId, Set<String> allowedExtensions, long maxFileSize) {
        Ressource resource = ressourceRepository.findByIdAndStructureId(resourceId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable"));
        Path file = resolveStoredResource(resource, structureId, allowedExtensions);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
        }

        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > maxFileSize) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
            }

            Path realFile = file.toRealPath();
            Path realUploadBase = uploadBase.toRealPath();
            Path realResourcesDir = realUploadBase.resolve("ressources").toRealPath();
            Path realStructureDir = realResourcesDir.resolve(structureId.toString()).toRealPath();
            if (!realStructureDir.startsWith(realResourcesDir) || !realFile.startsWith(realStructureDir)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
            }

            String filename = file.getFileName().toString();
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            return new ResourceFile(filename, contentTypeFor(extension), Files.readAllBytes(realFile));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de lire le fichier source", e);
        }
    }

    private String contentTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            default -> "application/octet-stream";
        };
    }

    private String extractText(Ressource resource, UUID structureId) {
        Path file = resolveStoredResource(resource, structureId);
        if (file == null) {
            return "";
        }
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file) > MAX_FILE_SIZE) {
                return "";
            }
            Path realFile = file.toRealPath();
            Path realUploadBase = uploadBase.toRealPath();
            Path realResourcesDir = realUploadBase.resolve("ressources").toRealPath();
            Path realStructureDir = realResourcesDir.resolve(structureId.toString()).toRealPath();
            if (!realStructureDir.startsWith(realResourcesDir) || !realFile.startsWith(realStructureDir)) {
                return "";
            }

            BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_PER_RESOURCE);
            try (InputStream input = Files.newInputStream(realFile)) {
                new AutoDetectParser().parse(input, handler, new Metadata(), new ParseContext());
            } catch (SAXException limitReached) {
                // Tika's bounded content handler may stop parsing after the allowed text length.
            }
            return handler.toString().trim();
        } catch (IOException | TikaException | RuntimeException ignored) {
            return "";
        }
    }

    private Path resolveStoredResource(Ressource resource, UUID structureId) {
        return resolveStoredResource(resource, structureId, PARSEABLE_EXTENSIONS);
    }

    private Path resolveStoredResource(
            Ressource resource, UUID structureId, Set<String> allowedExtensions) {
        String url = resource.getUrl();
        String expectedPrefix = "/fichiers/ressources/" + structureId + "/";
        if (url == null || !url.startsWith(expectedPrefix)) {
            return null;
        }
        String filename = url.substring(expectedPrefix.length());
        if (!filename.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9]+")
                || filename.contains("/") || filename.contains("\\")) {
            return null;
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            return null;
        }
        Path structureDir = uploadBase.resolve("ressources").resolve(structureId.toString()).normalize();
        Path candidate = structureDir.resolve(filename).normalize();
        return candidate.startsWith(structureDir) ? candidate : null;
    }

    private List<UUID> singletonOrPlaceholder(UUID id) {
        return id == null ? List.of(NO_MISSION_RESOURCE) : List.of(id);
    }

    private int scopePriority(PorteeRessource portee) {
        if (portee == null) return 5;
        return switch (portee) {
            case MISSION -> 0;
            case COHORTE -> 1;
            case PHASE -> 2;
            case PARCOURS -> 3;
            case STRUCTURE -> 4;
        };
    }
}
