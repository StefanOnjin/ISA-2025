import requests
import threading
import time

BASE_URL = "http://localhost:8080/api/videos"
VIDEO_ID = 1
NUMBER_OF_REQUESTS = 20

def watch_video(video_id):
    requests.get(f"{BASE_URL}/{video_id}")

if __name__ == "__main__":

    try:
        requests.get(BASE_URL)
    except:
        exit(1)

    response = requests.get(f"{BASE_URL}/{VIDEO_ID}")
    start_views = response.json()["views"]

    threads = []

    for i in range(NUMBER_OF_REQUESTS):
        t = threading.Thread(target=watch_video, args=(VIDEO_ID,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    time.sleep(1)

    response = requests.get(f"{BASE_URL}/{VIDEO_ID}")
    end_views = response.json()["views"]

    expected_views = start_views + NUMBER_OF_REQUESTS + 1

    print(f"Pocetni broj pregleda: {start_views}")
    print(f"Konacni broj pregleda: {end_views}")
    print(f"Ocekivani broj pregleda: {expected_views}")

    if end_views == expected_views:
        print("REZULTAT: Test je prosao")
    else:
        print("REZULTAT: Test nije prosao")
