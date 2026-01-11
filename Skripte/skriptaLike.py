import json
import threading
import time
import urllib.error
import urllib.request


BASE_URL = "http://localhost:8080"
VIDEO_ID = 1
USERS = [
    {"email": "milankecmanbp@gmail.com", "password": "Kecman187!"},
    {"email": "milankecman003@gmail.com", "password": "Kecman187!"},
]
WAIT_BEFORE_UNLIKE_SECONDS = 30


def http_json(method, url, payload=None, headers=None):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, body
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        return e.code, body
    except Exception as e:  
        return None, str(e)


def login(email, password):
    status, body = http_json(
        "POST",
        f"{BASE_URL}/auth/login",
        {"email": email, "password": password},
    )
    if status != 200:
        return None, status, body
    try:
        data = json.loads(body)
    except json.JSONDecodeError:
        return None, status, body
    return data.get("accessToken"), status, body


def like_or_unlike(method, token, results, idx, barrier, action_label):
    barrier.wait()
    start = time.perf_counter()
    status, body = http_json(
        method,
        f"{BASE_URL}/api/videos/{VIDEO_ID}/like",
        None,
        {"Authorization": f"Bearer {token}"},
    )
    elapsed_ms = (time.perf_counter() - start) * 1000.0
    likes_count = None
    try:
        data = json.loads(body)
        likes_count = data.get("likesCount")
    except Exception:
        pass
    results[idx] = (action_label, status, elapsed_ms, likes_count, body)


def run_phase(method, tokens, action_label):
    barrier = threading.Barrier(len(tokens))
    results = [None] * len(tokens)
    threads = []
    for i, token in enumerate(tokens):
        t = threading.Thread(
            target=like_or_unlike,
            args=(method, token, results, i, barrier, action_label),
        )
        t.start()
        threads.append(t)
    for t in threads:
        t.join()
    return results


def get_likes_count(token):
    status, body = http_json(
        "GET",
        f"{BASE_URL}/api/videos/{VIDEO_ID}",
        None,
        {"Authorization": f"Bearer {token}"},
    )
    if status != 200:
        return None, status, body
    try:
        data = json.loads(body)
        return data.get("likesCount"), status, body
    except Exception:
        return None, status, body


def main():
    print("Logging in users...")
    tokens = []
    for user in USERS:
        token, status, body = login(user["email"], user["password"])
        if not token:
            print(f"[LOGIN FAIL] {user['email']}: status={status}, body={body}")
            return
        tokens.append(token)
        print(f"[LOGIN OK] {user['email']}")

    likes_before, status, body = get_likes_count(tokens[0])
    print(f"Total likes BEFORE like phase: {likes_before} (status={status})")

    print("=== LIKE phase (concurrent) ===")
    like_results = run_phase("POST", tokens, "LIKE")
    for i, (label, status, ms, likes, body) in enumerate(like_results):
        print(
            f"User{i+1}-{label}: status={status}, time={ms:.2f}ms, likesCount={likes}, body={body}"
        )

    likes_after_like, status, body = get_likes_count(tokens[0])
    print(f"Total likes AFTER like phase: {likes_after_like} (status={status})")

    print(f"Waiting {WAIT_BEFORE_UNLIKE_SECONDS} seconds before UNLIKE...")
    time.sleep(WAIT_BEFORE_UNLIKE_SECONDS)

    print("=== UNLIKE phase (concurrent) ===")
    unlike_results = run_phase("DELETE", tokens, "UNLIKE")
    for i, (label, status, ms, likes, body) in enumerate(unlike_results):
        print(
            f"User{i+1}-{label}: status={status}, time={ms:.2f}ms, likesCount={likes}, body={body}"
        )

    likes_after_unlike, status, body = get_likes_count(tokens[0])
    print(f"Total likes AFTER unlike phase: {likes_after_unlike} (status={status})")


if __name__ == "__main__":
    main()
