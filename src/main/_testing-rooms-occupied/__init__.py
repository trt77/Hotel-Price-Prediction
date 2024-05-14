import pandas as pd
from datetime import datetime

# Load the CSV file
file_path = 'hotel_stays_large.csv'
hotel_stays = pd.read_csv(file_path)

# Function to count occupied rooms of a certain type on a given date
def count_occupied_rooms(stays_df, target_date, room_type):
    # Convert target_date to datetime
    target_date = datetime.strptime(target_date, '%Y-%m-%d')

    # Convert begin_of_stay and end_of_stay to datetime
    stays_df['begin_of_stay'] = pd.to_datetime(stays_df['begin_of_stay'])
    stays_df['end_of_stay'] = pd.to_datetime(stays_df['end_of_stay'])

    # Filter stays by room_type
    filtered_stays = stays_df[stays_df['room_type'] == room_type]

    # Count the number of stays where target_date is between begin_of_stay and end_of_stay (inclusive)
    occupied_rooms_count = filtered_stays[
        (filtered_stays['begin_of_stay'] <= target_date) &
        (filtered_stays['end_of_stay'] >= target_date)
        ].shape[0]

    return occupied_rooms_count

# Example usage
room_type = 'Single'  # Change as needed
target_date = '2021-08-22'  # Change as needed
occupied_rooms = count_occupied_rooms(hotel_stays, target_date, room_type)

print(f"Number of occupied '{room_type}' rooms on {target_date}: {occupied_rooms}")
