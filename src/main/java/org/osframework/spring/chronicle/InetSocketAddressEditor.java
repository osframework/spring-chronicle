package org.osframework.spring.chronicle;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.net.InetSocketAddress;

/**
 * Supports setting and displaying JavaBean properties of type
 * {@code InetSocketAddress}.
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 */
public class InetSocketAddressEditor extends PropertyEditorSupport {

    public InetSocketAddressEditor() {
        super();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Invalid argument - empty address");
        }
        String hostname = null;
        int port = 0;
        int lastColonIdx = text.lastIndexOf(':');
        boolean ipv6 = false;
        if (-1 != lastColonIdx) {
            for (int i = (lastColonIdx + 1); i < text.length(); i++) {
                if (']' == text.charAt(i)) {
                    ipv6 = true;
                    break;
                }
            }
            if (ipv6) {
                hostname = text;
            } else {
                hostname = text.substring(0, lastColonIdx);
                try {
                    port = Integer.parseInt(text.substring(lastColonIdx + 1));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Invalid argument - bad port number");
                }
            }
        } else {
            hostname = text;
        }
        setValue(new InetSocketAddress(hostname, port));
    }

    @Override
    public String getAsText() {
        InetSocketAddress value = (InetSocketAddress)getValue();
        return (null == value) ? null : value.toString();
    }

}
