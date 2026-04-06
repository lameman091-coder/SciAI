def get_source_weight(source_type):
    # High Priority: 1.0 (PubMed, Nature)
    if source_type in ["PubMed", "Nature"]:
        return 1.0
        
    # Medium: 0.7 (Medium blogs)
    elif source_type in ["Medium"]:
        return 0.7
        
    # Low: 0.4 (Wikipedia)
    elif source_type in ["Wikipedia", "Wiki"]:
        return 0.4
        
    return 0.5 # Default

def rank_results(results):
    ranked = []
    for res in results:
        # Convert FAISS L2 Distance to a basic Similarity Score
        # (Alternatively use Cosine similarity in FAISS directly, but L2 formula suffices here)
        similarity = 1.0 / (1.0 + res['distance'])
        
        weight = get_source_weight(res['metadata'].get('source_type', ''))
        
        final_score = similarity * weight
        
        res['final_score'] = final_score
        ranked.append(res)
        
    # Sort highest score first
    ranked.sort(key=lambda x: x['final_score'], reverse=True)
    return ranked
