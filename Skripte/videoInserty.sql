
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
    'seed-thumbnail.jpg' AS thumbnail_path,
    'seed-video.mp4' AS video_path,
    NOW() AS created_at,
    (35.0 + (70.0 - 35.0) * random()) AS latitude,
    (-10.0 + (40.0 - -10.0) * random()) AS longitude,
    owner.id AS user_id
FROM generate_series(1, 5000) AS gs
JOIN owner ON TRUE;
