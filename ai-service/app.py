from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import os

from model_trainer import predict_category, train_and_save_model
from anomaly_engine import detect_isolation_forest_anomaly

app = FastAPI(
    title="AI Smart Expense Tracker - ML Engine",
    description="Machine Learning Service powered by Scikit-Learn TF-IDF, Logistic Regression, and Isolation Forest",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class CategorizeRequest(BaseModel):
    text: str
    merchant: Optional[str] = None
    amount: Optional[float] = None

class CategorizeResponse(BaseModel):
    category: str
    confidence: float
    model_name: str
    merchant: Optional[str] = None

class AnomalyRequest(BaseModel):
    current_amount: float
    historical_amounts: List[float]
    category: Optional[str] = "General"

class AnomalyResponse(BaseModel):
    is_anomaly: bool
    anomaly_score: float
    reason: str
    method: str

@app.on_event("startup")
def startup_event():
    # Ensure model is trained on startup
    try:
        train_and_save_model()
    except Exception as e:
        print(f"Warning on startup model train: {e}")

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "ai-ml-service", "engine": "scikit-learn"}

@app.post("/ai/v1/categorize", response_model=CategorizeResponse)
def categorize_endpoint(req: CategorizeRequest):
    query_text = req.text.strip()
    if not query_text and req.merchant:
        query_text = req.merchant.strip()
    
    if not query_text:
        raise HTTPException(status_code=400, detail="Text or merchant must be provided.")
        
    category, confidence = predict_category(query_text)
    
    return CategorizeResponse(
        category=category,
        confidence=round(confidence, 4),
        model_name="TF-IDF + Logistic Regression (Scikit-Learn)",
        merchant=req.merchant if req.merchant else query_text.split()[0].title()
    )

@app.post("/ai/v1/detect-anomaly", response_model=AnomalyResponse)
def anomaly_endpoint(req: AnomalyRequest):
    is_anomaly, score, reason, method = detect_isolation_forest_anomaly(
        current_amount=req.current_amount,
        historical_amounts=req.historical_amounts,
        category=req.category
    )
    return AnomalyResponse(
        is_anomaly=is_anomaly,
        anomaly_score=round(score, 4),
        reason=reason,
        method=method
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
