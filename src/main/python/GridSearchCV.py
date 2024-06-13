import pandas as pd
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error
import numpy as np
import time

# Load the dataset
data = pd.read_csv('generate-test-dataset/hotel_stays_large.csv')

# Feature engineering
data['begin_of_stay'] = pd.to_datetime(data['begin_of_stay'])
data['end_of_stay'] = pd.to_datetime(data['end_of_stay'])
data['stay_length'] = (data['end_of_stay'] - data['begin_of_stay']).dt.days + 1
data['begin_of_stay'] = data['begin_of_stay'].map(pd.Timestamp.toordinal)
data['end_of_stay'] = data['end_of_stay'].map(pd.Timestamp.toordinal)

# Define features and target
features = data[['begin_of_stay', 'end_of_stay', 'persons', 'stay_length', 'room_type']]
features = pd.get_dummies(features, columns=['room_type'])  # One-hot encode room types
target = data['total_price']

# Train-test split
X_train, X_test, y_train, y_test = train_test_split(features, target, test_size=0.2, random_state=42)

# Define models and hyperparameters for GridSearchCV
models = {
    'Random Forest': (RandomForestRegressor(), {
        'n_estimators': [50, 100],
        'max_depth': [10, 20, None]
    }),
    'Gradient Boosting': (GradientBoostingRegressor(), {
        'n_estimators': [50, 100],
        'learning_rate': [0.01, 0.1],
        'max_depth': [3, 5, 7]
    }),
    'Linear Regression': (LinearRegression(), {
        'fit_intercept': [True, False],
        'positive': [True, False]
    })
}

results = []

# Perform GridSearchCV for each model
for model_name, (model, params) in models.items():
    print(f"Evaluating {model_name}...")
    start_time = time.time()

    grid_search = GridSearchCV(model, params, cv=5, scoring='neg_mean_squared_error', n_jobs=-1)
    grid_search.fit(X_train, y_train)

    training_time = time.time() - start_time
    best_model = grid_search.best_estimator_

    start_time = time.time()
    predictions = best_model.predict(X_test)
    evaluation_time = time.time() - start_time

    rmse = np.sqrt(mean_squared_error(y_test, predictions))

    results.append({
        'Model': model_name,
        'Best Params': grid_search.best_params_,
        'Training Time (s)': training_time,
        'Evaluation Time (s)': evaluation_time,
        'RMSE': rmse
    })

# Convert results to DataFrame for better readability
results_df = pd.DataFrame(results)

# Set display options to avoid truncation
pd.set_option('display.max_columns', None)
pd.set_option('display.expand_frame_repr', False)
pd.set_option('max_colwidth', None)

print(results_df)

# Save results to CSV
results_df.to_csv('model_evaluation_results.csv', index=False)
