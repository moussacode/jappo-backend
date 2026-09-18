package sn.jappo.jappo_backend.meeting.livekit;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LiveKitRoomClient {

    private static final Logger log = LoggerFactory.getLogger(LiveKitRoomClient.class);

    private final RoomServiceClient roomServiceClient;
    private final String apiKey;
    private final String apiSecret;
    private final String host;

    public LiveKitRoomClient(
            @Value("${livekit.api-key}") String apiKey,
            @Value("${livekit.api-secret}") String apiSecret,
            @Value("${livekit.host}") String host
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.host = host;
        this.roomServiceClient = RoomServiceClient.createClient(host, apiKey, apiSecret);
    }

    /**
     * Crée une nouvelle room LiveKit
     */
    public void createRoom(String roomIdentifier) {
        try {
            roomServiceClient.createRoom(roomIdentifier);
            log.info("LiveKit room created: {}", roomIdentifier);
        } catch (Exception e) {
            log.error("Failed to create LiveKit room: {}", roomIdentifier, e);
            throw new RuntimeException("Failed to create LiveKit room", e);
        }
    }

    /**
     * Génère un token d'accès pour un participant
     */
    public String generateToken(String roomIdentifier, UUID userId, boolean isHost) {
        try {
            AccessToken token = new AccessToken(apiKey, apiSecret);
            token.setIdentity(userId.toString());
            token.setName(userId.toString());

            token.addGrants(new io.livekit.server.RoomJoin(true), new io.livekit.server.RoomName(roomIdentifier));

            if (isHost) {
                token.addGrants(new io.livekit.server.RoomAdmin(true));
            }

            return token.toJwt();
        } catch (Exception e) {
            log.error("Failed to generate LiveKit token for user: {}", userId, e);
            throw new RuntimeException("Failed to generate LiveKit token", e);
        }
    }

    /**
     * Supprime une room LiveKit
     */
    public void deleteRoom(String roomIdentifier) {
        try {
            roomServiceClient.deleteRoom(roomIdentifier);
            log.info("LiveKit room deleted: {}", roomIdentifier);
        } catch (Exception e) {
            log.error("Failed to delete LiveKit room: {}", roomIdentifier, e);
            // Ne pas lancer d'exception pour éviter de bloquer le processus de nettoyage
        }
    }

    public String getHost() {
        return host;
    }
}
