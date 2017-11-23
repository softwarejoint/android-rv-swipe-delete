package com.softwarejoint.swipe;

class RowItem {
    final String text;
    final int id;

    RowItem(String text, int id) {
        this.text = text + " " + id;
        this.id = id;
    }
}
