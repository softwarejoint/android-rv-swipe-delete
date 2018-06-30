package com.softwarejoint.sample;

import java.util.UUID;

class RowItem {
    final String text;
    final int id;

    RowItem(int id) {
        this.id = id;
        text = "Item: " + UUID.randomUUID().toString().substring(0, 5) + " " + id;
    }
}