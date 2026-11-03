package io.github.takattowo.usercabridge;

final class StatusContentInsets {
    static final class Padding {
        final int left;
        final int top;
        final int right;
        final int bottom;

        private Padding(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private StatusContentInsets() { }

    static Padding add(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom,
            int insetLeft, int insetTop, int insetRight, int insetBottom) {
        return new Padding(paddingLeft + insetLeft, paddingTop + insetTop,
                paddingRight + insetRight, paddingBottom + insetBottom);
    }
}
