package com.searchlight.infra.index;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;

/**
 * Custom Lucene codec that configures HNSW parameters for vector search.
 * 
 * Uses Lucene99HnswVectorsFormat with configurable M and efConstruction parameters
 * for optimal vector search performance.
 */
public final class SearchlightCodec extends FilterCodec {
    
    private final KnnVectorsFormat vectorsFormat;
    
    /**
     * Creates a new SearchlightCodec with the specified HNSW parameters.
     *
     * @param M Maximum number of connections per layer (default: 16)
     * @param efConstruction Size of the dynamic candidate list during construction (default: 100)
     */
    public SearchlightCodec(int M, int efConstruction) {
        super("Searchlight", Codec.getDefault());
        this.vectorsFormat = new Lucene99HnswVectorsFormat(M, efConstruction);
    }
    
    @Override
    public KnnVectorsFormat knnVectorsFormat() {
        return vectorsFormat;
    }
}