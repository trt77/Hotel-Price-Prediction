import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_squared_error
import numpy as np

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

# Initialize models
models = {
    'Random Forest': RandomForestRegressor(),
    'Linear Regression': LinearRegression(),
    'Gradient Boosting': GradientBoostingRegressor()
}

# Train and evaluate models
results = {}
for model_name, model in models.items():
    model.fit(X_train, y_train)
    predictions = model.predict(X_test)
    rmse = np.sqrt(mean_squared_error(y_test, predictions))
    results[model_name] = rmse

# Display results
results_df = pd.DataFrame(list(results.items()), columns=['Model', 'RMSE'])
print(results_df)
