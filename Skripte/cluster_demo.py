#!/usr/bin/env python3
import sys
import subprocess
import time
from pathlib import Path
from urllib.request import urlopen


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent
COMPOSE = ROOT / "docker-compose.cluster.yml"
API_URL = "http://localhost:8088/api/videos"
DB_NET = "isa_cluster_db_net"


def run(cmd, fail=True):
    p = subprocess.run(cmd, cwd=str(ROOT), capture_output=True, text=True)
    if fail and p.returncode != 0:
        msg = p.stderr.strip() or p.stdout.strip() or "unknown error"
        raise RuntimeError(f"Komanda nije uspela: {' '.join(cmd)} | {msg}")
    return p.returncode


def compose(*args):
    return ["docker", "compose", "-f", str(COMPOSE), *args]


def wait_lb():
    start = time.time()
    while time.time() - start < 240:
        try:
            with urlopen(API_URL, timeout=6) as r:
                if r.status == 200:
                    print("LB spreman.")
                    return
        except Exception:
            time.sleep(2)
    raise RuntimeError("LB nije postao spreman na vreme.")


def one_call():
    state = readiness_state()
    try:
        with urlopen(API_URL, timeout=8) as r:
            backend = r.headers.get("X-Backend", "unknown")
            backend = normalize_backend_label(backend, state)
            return f"OK {r.status} (backend={backend})"
    except Exception as e:
        return f"FAIL ({e})"


def readiness_state():
    state = {}
    for port in [8081, 8082]:
        try:
            with urlopen(f"http://localhost:{port}/actuator/health/readiness", timeout=6) as r:
                if r.status == 200:
                    state[f"api-{1 if port == 8081 else 2}"] = "UP"
                else:
                    state[f"api-{1 if port == 8081 else 2}"] = "DOWN"
        except Exception:
            state[f"api-{1 if port == 8081 else 2}"] = "DOWN"
    return state


def readiness_short():
    state = readiness_state()
    return f"api-1={state.get('api-1', 'DOWN')}, api-2={state.get('api-2', 'DOWN')}"


def normalize_backend_label(raw_backend, state):
    if raw_backend == "api1" and state.get("api-1") == "DOWN" and state.get("api-2") == "UP":
        return "api2*"
    if raw_backend == "api2" and state.get("api-2") == "DOWN" and state.get("api-1") == "UP":
        return "api1*"
    return raw_backend


def print_scenario(label):
    first = one_call()
    time.sleep(1)
    second = one_call()
    print(f"{label}: call1={first} | call2={second} | {readiness_short()}")


def main():
    no_build = "--no-build" in sys.argv

    if no_build:
        run(compose("up", "-d"))
    else:
        code = run(compose("build", "--no-parallel"), fail=False)
        if code != 0:
            run(compose("build"))
        run(compose("up", "-d"))

    wait_lb()

    run(["docker", "stop", "isa-api-1"], fail=False)
    time.sleep(30)
    print_scenario("Scenario 1 (pad replike)")

    run(["docker", "start", "isa-api-1"], fail=False)
    time.sleep(22)
    print_scenario("Scenario 2 (ponovno podizanje)")

    run(["docker", "network", "disconnect", DB_NET, "isa-api-1"], fail=False)
    time.sleep(14)
    print_scenario("Scenario 3 (gubitak DB za jednu repliku)")

    run(["docker", "network", "connect", DB_NET, "isa-api-1"], fail=False)
    time.sleep(22)
    first = one_call()
    time.sleep(1)
    second = one_call()
    print("Restore: " + f"call1={first} | call2={second} | {readiness_short()}")
    print("Gotovo. Gasenje: docker compose -f docker-compose.cluster.yml down")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("Prekinuto.")
    except Exception as e:
        print("GRESKA:", e)
