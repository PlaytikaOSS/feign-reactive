package reactivefeign.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentType {

    private static final Pattern charsetPattern = Pattern
            .compile("(?i)\\bcharset=\\s*\"?([^\\s;\"/]*)/?>");


    private final String mediaType;
    private final Charset charset;

    private ContentType(String mediaType, Charset charset) {
        this.mediaType = mediaType;
        this.charset = charset;
    }

    public static ContentType parse(String contentType){
        int splitPos = contentType.indexOf(';');
        if(splitPos == -1){
            return new ContentType(contentType, StandardCharsets.UTF_8);
        }
        String mediaType = contentType.substring(0, splitPos).trim();
        Charset charset = getCharsetFromContentType(contentType.substring(splitPos + 1));
        return new ContentType(mediaType, charset);
    }

    private static Charset getCharsetFromContentType(CharSequence contentType) {
        if (contentType == null)
            return null;
        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            if (Charset.isSupported(charset))
                return Charset.forName(charset);
            charset = charset.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(charset))
                return Charset.forName(charset);
        }
        return StandardCharsets.UTF_8;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Charset getCharset() {
        return charset;
    }
}
