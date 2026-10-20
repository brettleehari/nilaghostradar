# Private media overrides

`app/src/release/res/raw/` and `app/src/debug/res/raw/` are gitignored. Drop `radar_ambience.wav`
(or `.mp3`, `.m4a`, `.ogg`) and/or `scare_track.*` there and rebuild; that build type uses your file
instead of the committed one. Never commit music you do not own.
