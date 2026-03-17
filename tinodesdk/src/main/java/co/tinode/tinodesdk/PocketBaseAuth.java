package co.tinode.tinodesdk;

import android.text.TextUtils;

import com.fasterxml.jackson.core.Base64Variants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class PocketBaseAuth {
    private static final int TINODE_PORT = 6060;
    private static final int POCKETBASE_PORT = 8090;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String USERS_COLLECTION = "users";
    private static final String ACCOUNT_SECRET_SCHEME = "pb-basic";

    private PocketBaseAuth() {
    }

    @NotNull
    static Session authenticateWithPasswordBlocking(String hostName, boolean tls,
                                                    String identity, String password) throws Exception {
        if (TextUtils.isEmpty(identity) || TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("PocketBase identity and password are required");
        }

        JSONObject payload = new JSONObject();
        payload.put("identity", identity);
        payload.put("password", password);

        URL url = new URL(resolveBaseUrl(hostName, tls) +
                "/api/collections/" + USERS_COLLECTION + "/auth-with-password");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            byte[] requestBody = payload.toString().getBytes(StandardCharsets.UTF_8);

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(requestBody);
            }

            int code = conn.getResponseCode();
            String body = readResponseBody(code >= HttpURLConnection.HTTP_OK &&
                    code < HttpURLConnection.HTTP_MULT_CHOICE ? conn.getInputStream() : conn.getErrorStream());

            if (code < HttpURLConnection.HTTP_OK || code >= HttpURLConnection.HTTP_MULT_CHOICE) {
                throw new IOException(extractError(code, body));
            }

            JSONObject json = new JSONObject(body);
            String token = json.optString("token", null);
            if (TextUtils.isEmpty(token)) {
                throw new IOException("PocketBase auth returned no token");
            }
            return new Session(token);
        } finally {
            conn.disconnect();
        }
    }

    @NotNull
    public static String encodeAccountSecret(String identity, String password) {
        if (identity == null) {
            throw new IllegalArgumentException("identity must not be null");
        }
        if (identity.contains(":")) {
            throw new IllegalArgumentException("illegal character ':' in identity '" + identity + "'");
        }
        password = password == null ? "" : password;
        return ACCOUNT_SECRET_SCHEME + ":" + Base64Variants.getDefaultVariant()
                .encode((identity + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    public static Credentials decodeAccountSecret(String encoded) {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }

        int splitAt = encoded.indexOf(':');
        if (splitAt <= 0 || splitAt >= encoded.length() - 1) {
            return null;
        }
        if (!ACCOUNT_SECRET_SCHEME.equals(encoded.substring(0, splitAt))) {
            return null;
        }

        String decoded = new String(Base64Variants.getDefaultVariant()
                .decode(encoded.substring(splitAt + 1)), StandardCharsets.UTF_8);
        int delimiter = decoded.indexOf(':');
        if (delimiter <= 0) {
            return null;
        }

        return new Credentials(decoded.substring(0, delimiter),
                delimiter == decoded.length() - 1 ? "" : decoded.substring(delimiter + 1));
    }

    @NotNull
    private static String resolveBaseUrl(String hostName, boolean tls) throws URISyntaxException {
        if (TextUtils.isEmpty(hostName)) {
            throw new URISyntaxException("", "Missing Tinode host name");
        }

        String candidate = hostName.contains("://") ? hostName : (tls ? "https://" : "http://") + hostName;
        URI tinodeUri = new URI(candidate);

        String host = tinodeUri.getHost();
        if (TextUtils.isEmpty(host)) {
            throw new URISyntaxException(hostName, "Invalid Tinode host name");
        }

        int port = tinodeUri.getPort();
        if (port <= 0 || port == TINODE_PORT) {
            port = POCKETBASE_PORT;
        }

        return new URI(tls ? "https" : "http", null, host, port, null, null, null).toString();
    }

    @NotNull
    private static String readResponseBody(@Nullable InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @NotNull
    private static String extractError(int code, String body) {
        if (!TextUtils.isEmpty(body)) {
            try {
                String message = new JSONObject(body).optString("message");
                if (!TextUtils.isEmpty(message)) {
                    return "PocketBase auth failed (" + code + "): " + message;
                }
            } catch (Exception ignored) {
            }
        }
        return "PocketBase auth failed (" + code + ")";
    }

    static final class Session {
        private final String mToken;

        Session(@NotNull String token) {
            mToken = token;
        }

        @NotNull
        String getToken() {
            return mToken;
        }
    }

    public static final class Credentials {
        private final String mIdentity;
        private final String mPassword;

        Credentials(@NotNull String identity, @NotNull String password) {
            mIdentity = identity;
            mPassword = password;
        }

        @NotNull
        public String getIdentity() {
            return mIdentity;
        }

        @NotNull
        public String getPassword() {
            return mPassword;
        }
    }
}
