import json
import queue
import threading
import time
from urllib.request import Request, urlopen


BASE_URL = "http://localhost:8080"
ENDPOINT = "/api/videos"

EMAIL = "strahinjaponjevic03@gmail.com"
PASSWORD = "sifra123"
USE_LOGIN = True
SEND_HEARTBEAT = True

DURATION_SECONDS = 20
THREADS = 64
TARGET_RPS = 200
TIMEOUT_SECONDS = 5
HEARTBEAT_INTERVAL_SECONDS = 20

request_queue = queue.Queue(maxsize=max(TARGET_RPS * 4, 1000))
stop_event = threading.Event()

stats_lock = threading.Lock()
total_requests = 0
ok_requests = 0
error_requests = 0


def login_get_token():
    url = BASE_URL + "/auth/login"
    body = json.dumps({"email": EMAIL, "password": PASSWORD}).encode("utf-8")
    req = Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    with urlopen(req, timeout=TIMEOUT_SECONDS) as res:
        data = json.loads(res.read().decode("utf-8"))
    return data.get("accessToken")


def do_request(auth_header):
    req = Request(BASE_URL + ENDPOINT, method="GET")
    if auth_header:
        req.add_header("Authorization", auth_header)
    with urlopen(req, timeout=TIMEOUT_SECONDS) as res:
        res.read()


def worker(auth_header):
    global total_requests, ok_requests, error_requests

    while True:
        try:
            request_queue.get(timeout=0.5)
        except queue.Empty:
            if stop_event.is_set():
                return
            continue

        try:
            do_request(auth_header)
            with stats_lock:
                total_requests += 1
                ok_requests += 1
        except Exception:
            with stats_lock:
                total_requests += 1
                error_requests += 1
        finally:
            request_queue.task_done()


def scheduler():
    total_slots = DURATION_SECONDS * TARGET_RPS
    start = time.perf_counter()

    for i in range(total_slots):
        if stop_event.is_set():
            break
        planned = start + (i / TARGET_RPS)
        now = time.perf_counter()
        if planned > now:
            time.sleep(planned - now)
        request_queue.put(1)


def heartbeat_loop(auth_header):
    if not auth_header:
        return
    while not stop_event.is_set():
        try:
            req = Request(BASE_URL + "/api/monitoring/heartbeat", method="POST")
            req.add_header("Authorization", auth_header)
            with urlopen(req, timeout=TIMEOUT_SECONDS) as res:
                res.read()
        except Exception:
            pass
        time.sleep(HEARTBEAT_INTERVAL_SECONDS)


def send_logout(auth_header):
    if not auth_header:
        return
    try:
        req = Request(BASE_URL + "/api/monitoring/logout", method="POST")
        req.add_header("Authorization", auth_header)
        with urlopen(req, timeout=TIMEOUT_SECONDS) as res:
            res.read()
        print("[auth] logout signal sent")
    except Exception as ex:
        print(f"[auth] logout signal failed: {ex}")


def main():
    auth_header = None
    if USE_LOGIN:
        print("[auth] login...")
        token = login_get_token()
        if not token:
            raise RuntimeError("Login nije vratio accessToken.")
        auth_header = f"Bearer {token}"
        print("[auth] login ok")

    workers = []
    for _ in range(THREADS):
        t = threading.Thread(target=worker, args=(auth_header,), daemon=True)
        t.start()
        workers.append(t)

    hb_thread = None
    if SEND_HEARTBEAT and auth_header:
        hb_thread = threading.Thread(target=heartbeat_loop, args=(auth_header,), daemon=True)
        hb_thread.start()

    print(
        f"[run] duration={DURATION_SECONDS}s threads={THREADS} "
        f"target_rps={TARGET_RPS} endpoint={ENDPOINT}"
    )
    start = time.perf_counter()

    scheduler_thread = threading.Thread(target=scheduler, daemon=True)
    scheduler_thread.start()
    scheduler_thread.join()

    request_queue.join()
    stop_event.set()

    for t in workers:
        t.join(timeout=1)
    if hb_thread:
        hb_thread.join(timeout=1)
    send_logout(auth_header)

    elapsed = time.perf_counter() - start
    with stats_lock:
        total = total_requests
        ok = ok_requests
        err = error_requests

    achieved_rps = total / elapsed if elapsed > 0 else 0.0

    print("\n=== Rezultat ===")
    print(f"Trajanje:          {elapsed:.2f}s")
    print(f"Ukupno zahteva:    {total}")
    print(f"Uspesni:           {ok}")
    print(f"Greske:            {err}")
    print(f"Ciljani RPS:       {TARGET_RPS}")
    print(f"Ostvareni RPS:     {achieved_rps:.2f}")

    if err == 0 and achieved_rps >= TARGET_RPS:
        print("PASS: bez gresaka i ostvaren ciljani RPS")
    elif err == 0:
        print("PARTIAL: bez gresaka, ali ciljani RPS nije dostignut")
    else:
        print("FAIL: postoje greske tokom opterecenja")


if __name__ == "__main__":
    main()
