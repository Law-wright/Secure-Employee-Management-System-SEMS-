(function () {
    var root = document.getElementById("login-feedback");
    if (!root) {
        return;
    }

    var announcer = document.getElementById("login-announcer");
    var deadline = null;
    var serverOffset = 0;
    var tickTimer = null;
    var pollTimer = null;
    var wasLocked = false;

    function unit(count, word) {
        return count + " " + word + (count === 1 ? "" : "s");
    }

    function formatCountdown(remainingMs) {
        var totalSeconds = Math.max(0, Math.ceil(remainingMs / 1000));
        var hours = Math.floor(totalSeconds / 3600);
        var minutes = Math.floor((totalSeconds % 3600) / 60);
        var seconds = totalSeconds % 60;
        if (hours > 0) {
            return unit(hours, "hour") + " " + unit(minutes, "minute") + " " + unit(seconds, "second");
        }
        return unit(minutes, "minute") + " " + unit(seconds, "second");
    }

    function remainingMs() {
        if (deadline == null || Number.isNaN(deadline)) {
            return 0;
        }
        return deadline - (Date.now() + serverOffset);
    }

    function renderCountdown() {
        var countdown = document.getElementById("lockout-countdown");
        if (!countdown) {
            return;
        }
        countdown.textContent = formatCountdown(remainingMs());
    }

    function hideLockout() {
        var notice = document.getElementById("lockout-notice");
        if (notice) {
            notice.remove();
        }
        if (wasLocked && announcer) {
            announcer.textContent = "The lockout has ended. You can try signing in again.";
        }
        wasLocked = false;
        deadline = null;
        root.dataset.locked = "false";
        delete root.dataset.lockedUntil;
    }

    function showAttempts(status) {
        var current = document.getElementById("attempts-remaining");
        if (!status.showRemainingAttempts) {
            if (current) {
                current.remove();
            }
            return;
        }
        if (!current) {
            current = document.createElement("p");
            current.id = "attempts-remaining";
            current.className = "message error";
            current.setAttribute("role", "status");
            current.setAttribute("aria-live", "polite");
            root.appendChild(current);
        }
        var count = status.remainingAttempts;
        current.textContent = count === 1
            ? "1 login attempt remaining."
            : count + " login attempts remaining.";
    }

    function apply(status) {
        serverOffset = Date.parse(status.serverNow) - Date.now();
        if (status.locked && status.lockedUntil) {
            deadline = Date.parse(status.lockedUntil);
            root.dataset.locked = "true";
            root.dataset.lockedUntil = status.lockedUntil;
            root.dataset.serverNow = status.serverNow;
            wasLocked = true;
            renderCountdown();
            scheduleTick();
            schedulePoll();
        } else {
            window.clearTimeout(tickTimer);
            window.clearTimeout(pollTimer);
            hideLockout();
        }
        showAttempts(status);
    }

    function scheduleTick() {
        window.clearTimeout(tickTimer);
        tickTimer = window.setTimeout(function () {
            if (remainingMs() <= 0) {
                refresh();
                return;
            }
            renderCountdown();
            scheduleTick();
        }, 1000);
    }

    function schedulePoll() {
        window.clearTimeout(pollTimer);
        pollTimer = window.setTimeout(refresh, 2000);
    }

    function refresh() {
        fetch("/login/status", {
            headers: { "Accept": "application/json" },
            credentials: "same-origin",
            cache: "no-store"
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("status");
            }
            return response.json();
        }).then(apply).catch(function () {
            schedulePoll();
        });
    }

    if (root.dataset.locked === "true" && root.dataset.lockedUntil) {
        serverOffset = Date.parse(root.dataset.serverNow) - Date.now();
        deadline = Date.parse(root.dataset.lockedUntil);
        wasLocked = true;
        renderCountdown();
        scheduleTick();
        schedulePoll();
    }
})();
