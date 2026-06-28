package cc.hrva.ladica.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String UNKNOWN_IP = "unknown";

    public String resolve(final HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        return StringUtils.defaultIfBlank(request.getRemoteAddr(), UNKNOWN_IP);
    }

}
