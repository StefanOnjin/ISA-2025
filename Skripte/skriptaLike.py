import requests
import threading
import time

BASE_URL = "http://localhost:8080"
VIDEO_ID = 1
USERS = [
    {"email": "milankecmanbp@gmail.com", "password": "Kecman187!"},
    {"email": "milankecman003@gmail.com", "password": "Kecman187!"},
]
WAIT_BEFORE_UNLIKE_SECONDS = 30


def like_video(token):
    headers = {"Authorization": f"Bearer {token}"}
    requests.post(f"{BASE_URL}/api/videos/{VIDEO_ID}/like", headers=headers, timeout=10)


def unlike_video(token):
    headers = {"Authorization": f"Bearer {token}"}
    requests.delete(f"{BASE_URL}/api/videos/{VIDEO_ID}/like", headers=headers, timeout=10)


def login_all():
    tokens = []
    for user in USERS:
        resp = requests.post(
            f"{BASE_URL}/auth/login",
            json={"email": user["email"], "password": user["password"]},
            timeout=10,
        )
        if resp.status_code != 200:
            print(f"Login failed for {user['email']}: {resp.status_code} {resp.text}")
            return None
        tokens.append(resp.json().get("accessToken"))
    return tokens


def get_likes(token):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    resp = requests.get(f"{BASE_URL}/api/videos/{VIDEO_ID}", headers=headers, timeout=10)
    return resp.json().get("likesCount", 0)


def run_concurrent(fn, tokens):
    threads = []
    for token in tokens:
        t = threading.Thread(target=fn, args=(token,))
        t.start()
        threads.append(t)
    for t in threads:
        t.join()


if __name__ == "__main__":
 
    try:
        requests.get(BASE_URL, timeout=5)
    except Exception as e:
        print(f"API not reachable: {e}")
        exit(1)

    tokens = login_all()
    if not tokens:
        exit(1)

    start_likes = get_likes(tokens[0])
    print(f"Pocetni broj lajkova: {start_likes}")

    run_concurrent(like_video, tokens)
    time.sleep(1)
    after_like = get_likes(tokens[0])
    expected_after_like = start_likes + len(tokens)
    print(f"Konacni broj lajkova posle LIKE: {after_like}")
    print(f"Ocekivani broj lajkova posle LIKE: {expected_after_like}")

    print(f"Cekam {WAIT_BEFORE_UNLIKE_SECONDS} sekundi pre UNLIKE...")
    time.sleep(WAIT_BEFORE_UNLIKE_SECONDS)

    run_concurrent(unlike_video, tokens)
    time.sleep(1)
    after_unlike = get_likes(tokens[0])
    print(f"Konacni broj lajkova posle UNLIKE: {after_unlike}")
    print(f"Ocekivani broj lajkova posle UNLIKE: {start_likes}")

    if after_like == expected_after_like and after_unlike == start_likes:
        print("REZULTAT: Test je prosao")
    else:
        print("REZULTAT: Test nije prosao")
