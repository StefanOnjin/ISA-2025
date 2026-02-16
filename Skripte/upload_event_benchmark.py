import os
import time
from pathlib import Path

import requests


BASE_URL = os.getenv("BASE_URL", "http://localhost:8080")
EMAIL = os.getenv("BENCH_EMAIL", "totorinja@gmail.com")
PASSWORD = os.getenv("BENCH_PASSWORD", "stefke123")
UPLOAD_COUNT = int(os.getenv("UPLOAD_COUNT", "50"))

SCRIPT_DIR = Path(__file__).resolve().parent
THUMB_PATH = Path(os.getenv("THUMB_PATH", str(SCRIPT_DIR / "horse.jpg"))).expanduser()
VIDEO_PATH = Path(os.getenv("VIDEO_PATH", str(SCRIPT_DIR / "video4.mp4"))).expanduser()

if not THUMB_PATH.is_absolute():
    THUMB_PATH = (Path.cwd() / THUMB_PATH).resolve()
if not VIDEO_PATH.is_absolute():
    VIDEO_PATH = (Path.cwd() / VIDEO_PATH).resolve()


def login():
    response = requests.post(
        f"{BASE_URL}/auth/login",
        json={"email": EMAIL, "password": PASSWORD},
        timeout=20,
    )
    response.raise_for_status()
    token = response.json().get("accessToken")
    if not token:
        raise RuntimeError("Login nije vratio accessToken")
    return token


def upload_one(index, token):
    headers = {"Authorization": f"Bearer {token}"}
    data = {
        "title": f"Upload benchmark {index}",
        "description": "Benchmark poruka za MQ JSON vs Protobuf",
        "tags": "benchmark,mq,upload",
        "durationSeconds": "12",
        "latitude": "44.8176",
        "longitude": "20.4633",
    }

    with open(THUMB_PATH, "rb") as thumb, open(VIDEO_PATH, "rb") as video:
        files = {
            "thumbnail": (THUMB_PATH.name, thumb, "image/jpeg"),
            "video": (VIDEO_PATH.name, video, "video/mp4"),
        }
        response = requests.post(
            f"{BASE_URL}/api/videos/upload",
            headers=headers,
            data=data,
            files=files,
            timeout=120,
        )
    return response.status_code, response.text


def main():
    print(f"Login korisnik: {EMAIL}")
    token = login()
    print(f"Start upload benchmark: {UPLOAD_COUNT} poruka")
    print(f"Thumbnail: {THUMB_PATH}")
    print(f"Video: {VIDEO_PATH}")

    ok = 0
    failed = 0
    start = time.perf_counter()
    for i in range(1, UPLOAD_COUNT + 1):
        try:
            status, _ = upload_one(i, token)
            if status in (200, 202):
                ok += 1
                print(f"[{i}/{UPLOAD_COUNT}] OK ({status})")
            else:
                failed += 1
                print(f"[{i}/{UPLOAD_COUNT}] FAIL ({status})")
        except Exception as ex:
            failed += 1
            print(f"[{i}/{UPLOAD_COUNT}] FAIL ({ex})")

    elapsed = time.perf_counter() - start
    print("\n=== Rezultat ===")
    print(f"Poslato: {UPLOAD_COUNT}")
    print(f"Uspeh: {ok}")
    print(f"Greske: {failed}")
    print(f"Vreme: {elapsed:.2f}s")
    print("\nProvera benchmark app: http://localhost:8090/benchmark/report")


if __name__ == "__main__":
    main()
