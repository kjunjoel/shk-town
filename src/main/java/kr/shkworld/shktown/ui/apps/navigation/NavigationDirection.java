package kr.shkworld.shktown.ui.apps.navigation;

import kr.shkworld.shktown.core.navigation.model.Position;

public enum NavigationDirection {
    UP("위로 이동 ⬆"),
    DOWN("아래로 이동 ⬇"),
    FORWARD("앞으로 이동 ⬆"),
    RIGHT("오른쪽으로 이동 ➡"),
    BACKWARD("뒤로 이동 ⬇"),
    LEFT("왼쪽으로 이동 ⬅");

    private final String text;

    NavigationDirection(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static NavigationDirection calculate(Position from, Position to, double threshold) {
        if (from == null || to == null) return FORWARD;

        double y = to.y() - from.y();
        if (y >= threshold) return UP;
        if (y <= -threshold) return DOWN;

        double angle = Math.toDegrees(Math.atan2(-(to.x() - from.x()), to.z() - from.z())) - from.yaw();
        angle = ((angle + 540) % 360) - 180;

        if (angle >= -45 && angle < 45) return FORWARD;
        if (angle >= 45 && angle < 135) return RIGHT;
        return angle >= -135 && angle < -45 ? LEFT : BACKWARD;
    }
}
