import numpy as np
from sklearn.ensemble import IsolationForest
from typing import List, Tuple, Optional

def detect_isolation_forest_anomaly(
    current_amount: float,
    historical_amounts: List[float],
    category: str = ""
) -> Tuple[bool, float, str, str]:
    """
    Evaluates whether current_amount is an anomaly using Scikit-Learn's Isolation Forest.
    Returns: (is_anomaly, anomaly_score, reason, method)
    """
    if not historical_amounts or len(historical_amounts) < 3:
        return (
            False,
            0.0,
            "Gracefully skipped: Insufficient historical transaction records (< 3) to fit Isolation Forest.",
            "Isolation Forest (Insufficient Data Graceful Fallback)"
        )
    
    # Filter valid positive numbers
    clean_history = [float(x) for x in historical_amounts if x is not None and float(x) > 0]
    
    if len(clean_history) < 3:
        return (
            False,
            0.0,
            "Gracefully skipped: Insufficient non-zero historical records to establish baseline.",
            "Isolation Forest (Insufficient Data)"
        )

    # Reshape for Isolation Forest
    X_train = np.array(clean_history).reshape(-1, 1)
    
    # Calculate contamination safely based on data size
    contamination = min(0.1, max(0.01, 1.0 / len(clean_history)))
    
    clf = IsolationForest(
        contamination=contamination,
        random_state=42,
        n_estimators=100
    )
    clf.fit(X_train)
    
    X_test = np.array([[float(current_amount)]])
    prediction = clf.predict(X_test)[0] # -1 for anomaly, 1 for normal
    raw_score = clf.decision_function(X_test)[0] # Negative for anomalies
    
    # Normalize score between 0.0 and 1.0 (higher = more anomalous)
    normalized_score = float(max(0.0, min(1.0, 0.5 - (raw_score * 2.0))))
    
    mean_val = float(np.mean(clean_history))
    std_val = float(np.std(clean_history))
    
    is_anomaly = bool(prediction == -1 and (current_amount > mean_val))
    
    if is_anomaly:
        multiplier = current_amount / (mean_val if mean_val > 0 else 1.0)
        reason = (
            f"Isolation Forest flagged anomaly: Transaction of ₹{current_amount:,.2f} "
            f"is {multiplier:.1f}x higher than historical average (₹{mean_val:,.2f})."
        )
    else:
        reason = f"Transaction of ₹{current_amount:,.2f} is consistent with normal spending patterns."
        
    return is_anomaly, normalized_score, reason, "Isolation Forest (Scikit-learn)"
