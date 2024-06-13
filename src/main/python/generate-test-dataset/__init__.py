import csv
import random
import datetime

# Constants
NUM_YEARS = 7
NUM_ROOMS = 150
OCCUPANCY_RATE = 0.75  # 75% occupancy
SEASONS = {
    'high': 1.7,
    'average': 1.0,
    'low': 0.5
}
BASE_PRICE = 150  # base price per night for the average season
DAYS_PER_YEAR = 365
YEARLY_INFLATION_RATE = 0.03  # 3% annual inflation

# Date ranges for seasons
SEASON_DATES = {
    'high': [(datetime.date(2024, 12, 15), datetime.date(2024, 12, 31)), (datetime.date(2024, 7, 1), datetime.date(2024, 8, 31))],
    'low': [(datetime.date(2024, 1, 1), datetime.date(2024, 3, 15)), (datetime.date(2024, 10, 1), datetime.date(2024, 11, 30))],
    'average': [(datetime.date(2024, 3, 16), datetime.date(2024, 6, 30)), (datetime.date(2024, 9, 1), datetime.date(2024, 9, 30)), (datetime.date(2024, 12, 1), datetime.date(2024, 12, 14))]
}

# Date ranges for events
EVENT_DATES = [
    (datetime.date(2024, 6, 25), datetime.date(2024, 6, 27)),  # Example event
    (datetime.date(2024, 12, 5), datetime.date(2024, 12, 7)),  # Another event
]

ROOM_TYPES = ['Single', 'Double', 'Family']

def get_season(date):
    for season, date_ranges in SEASON_DATES.items():
        for start_date, end_date in date_ranges:
            if start_date <= date <= end_date:
                return season
    return 'average'  # default season

def is_event_date(date):
    for start_date, end_date in EVENT_DATES:
        if start_date <= date <= end_date:
            return True
    return False

def generate_stay_id(index):
    return f'STAY{index:07d}'

def generate_dates(start_year):
    start_date = datetime.date(start_year, 1, 1) + datetime.timedelta(days=random.randint(0, DAYS_PER_YEAR - 1))
    stay_length = random.choices([1, 2, 3], weights=[0.7, 0.2, 0.1], k=1)[0]
    end_date = start_date + datetime.timedelta(days=stay_length - 1)
    return start_date, end_date, stay_length

def calculate_total_price(start_date, end_date, stay_length, start_year):
    season = get_season(start_date)
    season_multiplier = SEASONS[season]
    nightly_price = BASE_PRICE * season_multiplier

    # Apply event spike
    if is_event_date(start_date):
        nightly_price *= 2  # Double the price during events

    # Apply yearly inflation
    years_since_start = datetime.datetime.now().year - start_year
    nightly_price *= (1 + YEARLY_INFLATION_RATE) ** years_since_start

    total_price = nightly_price * stay_length

    # Apply discount for longer stays
    if stay_length == 2:
        total_price *= 0.9
    elif stay_length == 3:
        total_price *= 0.8

    # Add a small random variation
    total_price += random.uniform(-5, 5)

    return round(total_price, 2)

def generate_persons(room_type):
    if room_type == 'Single':
        return 1
    elif room_type == 'Double':
        return random.randint(1, 2)
    elif room_type == 'Family':
        return random.randint(2, 4)

def generate_csv(filename):
    with open(filename, mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(['stay_id', 'begin_of_stay', 'end_of_stay', 'persons', 'room_type', 'total_price'])

        stay_id_counter = 0
        for year in range(datetime.datetime.now().year - NUM_YEARS, datetime.datetime.now().year):
            for _ in range(int(NUM_ROOMS * OCCUPANCY_RATE * DAYS_PER_YEAR)):
                stay_id = generate_stay_id(stay_id_counter)
                begin_of_stay, end_of_stay, stay_length = generate_dates(year)
                room_type = random.choice(ROOM_TYPES)
                persons = generate_persons(room_type)
                total_price = calculate_total_price(begin_of_stay, end_of_stay, stay_length, year)

                writer.writerow([stay_id, begin_of_stay, end_of_stay, persons, room_type, total_price])
                stay_id_counter += 1

generate_csv('hotel_stays_large.csv')
