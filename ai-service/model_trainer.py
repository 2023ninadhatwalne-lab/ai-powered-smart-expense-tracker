import os
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.model_selection import cross_val_score
import joblib

DATASET_PATH = os.path.join(os.path.dirname(__file__), "dataset.csv")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")
MODEL_FILE = os.path.join(MODEL_DIR, "category_model.joblib")

def train_and_save_model():
    os.makedirs(MODEL_DIR, exist_ok=True)
    
    if not os.path.exists(DATASET_PATH):
        raise FileNotFoundError(f"Dataset not found at {DATASET_PATH}")
        
    df = pd.read_csv(DATASET_PATH)
    df['text'] = df['text'].astype(str).str.lower().str.strip()
    
    X = df['text']
    y = df['category']
    
    pipeline = Pipeline([
        ('tfidf', TfidfVectorizer(
            ngram_range=(1, 2),
            sublinear_tf=True,
            strip_accents='unicode',
            lowercase=True,
            min_df=1
        )),
        ('clf', LogisticRegression(
            C=3.0,
            max_iter=1000,
            solver='lbfgs',
            random_state=42
        ))
    ])
    
    # Train full pipeline
    pipeline.fit(X, y)
    
    # Save model artifact
    joblib.dump(pipeline, MODEL_FILE)
    print(f"Model successfully trained on {len(df)} samples and saved to: {MODEL_FILE}")
    return pipeline

def predict_category(text: str):
    if not os.path.exists(MODEL_FILE):
        pipeline = train_and_save_model()
    else:
        pipeline = joblib.load(MODEL_FILE)
        
    cleaned = text.lower().strip()
    probs = pipeline.predict_proba([cleaned])[0]
    classes = pipeline.classes_
    
    best_idx = np.argmax(probs)
    predicted_category = classes[best_idx]
    confidence = float(probs[best_idx])
    
    return predicted_category, confidence

if __name__ == "__main__":
    model = train_and_save_model()
    test_queries = [
        "Swiggy dinner 450",
        "Uber ride to airport 600",
        "Netflix monthly 649",
        "Apollo pharmacy tablet 230",
        "Electricity bill payment 2100"
    ]
    print("\n--- Testing Model Predictions ---")
    for q in test_queries:
        cat, conf = predict_category(q)
        print(f"Query: '{q}' -> Predicted: {cat} (Confidence: {conf:.2%})")
