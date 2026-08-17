package kr.shkworld.shktown.ui.apps.navigation;

public enum NavigationSortType {
    NAME_ASC("오름차순"),
    NAME_DESC("내림차순"),
    DISTANCE_ASC("가까운순"),
    DISTANCE_DESC("먼순");

    private final String displayName;

    NavigationSortType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NavigationSortType next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
