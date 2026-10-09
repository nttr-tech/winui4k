namespace RibbonSpace.Model { public enum RibbonIconKind { Glyph, Path, Image, Text } public sealed record RibbonIcon(RibbonIconKind Kind, string Value, double ViewBoxSize = 24); }
