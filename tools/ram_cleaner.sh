#!/system/bin/sh
# 10-Minute Idle App RAM Cleaner for Android / Google TV
# Monitors heavy streaming apps and force-stops them when idle in background for >= 10 mins.

APPS="com.google.android.youtube.tv in.startv.hotstar com.netflix.ninja com.amazon.amazonvideo.livingroom com.apple.atve.androidtv.appletv com.crunchyroll.crunchyroid com.sonyliv com.graymatrix.did com.jio.media.stb.ondemand tv.twitch.android.app org.videolan.vlc com.mxtech.videoplayer.television"

IDLE_DIR="/data/local/tmp/app_idle"
mkdir -p "$IDLE_DIR"

while true; do
    NOW=$(date +%s)
    # Current resumed foreground activity package
    FOCUS=$(dumpsys activity activities 2>/dev/null | grep -m 1 "mResumedActivity" | sed -n 's/.*u0 \([^/]*\).*/\1/p')

    if [ -n "$FOCUS" ]; then
        echo "$NOW" > "$IDLE_DIR/$FOCUS"
    fi

    for APP in $APPS; do
        if ps -A | grep -q "$APP"; then
            if [ "$APP" = "$FOCUS" ]; then
                echo "$NOW" > "$IDLE_DIR/$APP"
            else
                LAST_SEEN=0
                if [ -f "$IDLE_DIR/$APP" ]; then
                    LAST_SEEN=$(cat "$IDLE_DIR/$APP" 2>/dev/null)
                fi

                if [ "$LAST_SEEN" -eq 0 ]; then
                    echo "$NOW" > "$IDLE_DIR/$APP"
                elif [ $((NOW - LAST_SEEN)) -ge 600 ]; then
                    # Force stop idle background app to free RAM and eliminate frozen state bugs
                    am force-stop "$APP" 2>/dev/null
                    rm -f "$IDLE_DIR/$APP"
                fi
            fi
        else
            rm -f "$IDLE_DIR/$APP"
        fi
    done

    sleep 60
done
