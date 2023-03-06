package io.airbyte.integrations.destination.starrocks.http;

import io.airbyte.integrations.destination.starrocks.io.StreamLoadStream;
import io.airbyte.protocol.models.v0.AirbyteRecordMessage;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class StreamLoadEntity extends AbstractHttpEntity {

    private static final Logger log = LoggerFactory.getLogger(StreamLoadEntity.class);

    protected static final int OUTPUT_BUFFER_SIZE = 2048;

    private static final Header CONTENT_TYPE =
            new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString());

    private final List<AirbyteRecordMessage> records;
    private final InputStream content;

    private final boolean chunked;
    private final long contentLength;


    public StreamLoadEntity(List<AirbyteRecordMessage> records) {
        this.records = records;
        this.content = new StreamLoadStream(records.listIterator());
        this.chunked = true;
        this.contentLength = -1L;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isChunked() {
        return chunked;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public Header getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public Header getContentEncoding() {
        return null;
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return content;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        long total = 0;
        try (InputStream inputStream = this.content) {
            final byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
            int l;
            while ((l = inputStream.read(buffer)) != -1) {
                total += l;
                outputStream.write(buffer, 0, l);
            }
        }
        log.info("Entity write end, contentLength : {}, total : {}", contentLength, total);
    }

    @Override
    public boolean isStreaming() {
        return true;
    }
}
