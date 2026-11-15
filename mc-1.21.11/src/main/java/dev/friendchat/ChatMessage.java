package dev.friendchat;

public record ChatMessage(long time, String user, String text, boolean system, String color, String icon) {
}
