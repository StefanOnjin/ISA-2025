import os
import shutil


def main() -> int:
    script_dir = os.path.dirname(os.path.abspath(__file__))
    storage_base = os.path.abspath(os.path.join(script_dir, "..", "back", "storage"))

    thumb_src = os.path.join(script_dir, "horse.jpg")
    video_src = os.path.join(script_dir, "video4.mp4")

    if not os.path.isfile(thumb_src):
        print(f"Missing thumbnail: {thumb_src}")
        return 1
    if not os.path.isfile(video_src):
        print(f"Missing video: {video_src}")
        return 1

    thumb_dest_dir = os.path.join(storage_base, "thumbnails")
    video_dest_dir = os.path.join(storage_base, "videos")
    os.makedirs(thumb_dest_dir, exist_ok=True)
    os.makedirs(video_dest_dir, exist_ok=True)

    shutil.copy2(thumb_src, os.path.join(thumb_dest_dir, "horse.jpg"))
    shutil.copy2(video_src, os.path.join(video_dest_dir, "video4.mp4"))

    print(f"Copied to {storage_base}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

"""
SQL SKRIPTA NAKOV OVOG IZVRSAVANJA POKRENUTI U PG ADMINU
WITH owner AS (
    SELECT id
    FROM users
    WHERE email = 'milankecmanbp@gmail.com'
    LIMIT 1
)
INSERT INTO videos (
    title,
    description,
    tags,
    views,
    thumbnail_path,
    video_path,
    created_at,
    latitude,
    longitude,
    user_id
)
SELECT
    'Test video ' || gs::text AS title,
    'Auto generated for tiles map.' AS description,
    'test,map' AS tags,
    0 AS views,
    'horse.jpg' AS thumbnail_path,
    'video4.mp4' AS video_path,
    NOW() AS created_at,
    (35.0 + (70.0 - 35.0) * random()) AS latitude,
    (-10.0 + (40.0 - -10.0) * random()) AS longitude,
    owner.id AS user_id
FROM generate_series(1, 5000) AS gs
JOIN owner ON TRUE;

"""