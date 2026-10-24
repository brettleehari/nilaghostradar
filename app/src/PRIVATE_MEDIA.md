# Private media overrides

`app/src/release/res/raw/` and `app/src/debug/res/raw/` are gitignored. Drop `radar_ambience.wav`, `radar_ambience_2.wav`, `radar_ambience_3.wav`
(or `.mp3`, `.m4a`, `.ogg`) and/or `scare_track.*`, `scare_track_2.*`, `scare_track_3.*` there and rebuild; that build type uses your file
instead of the committed one. Never commit music you do not own.
