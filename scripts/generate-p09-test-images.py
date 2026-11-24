#!/usr/bin/env python3
import os
import random
import struct
import sys
import zlib


FONT = {
    "0": ("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    "1": ("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    "2": ("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    "3": ("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    "4": ("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    "5": ("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    "6": ("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    "7": ("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    "8": ("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    "9": ("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    "x": ("00000", "10001", "01010", "00100", "01010", "10001", "00000"),
    "H": ("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    "O": ("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    "N": ("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    "a": ("00000", "00000", "01110", "00001", "01111", "10001", "01111"),
    "C": ("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
    "l": ("01100", "00100", "00100", "00100", "00100", "00100", "01110"),
    "-": ("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
    "+": ("00000", "00100", "00100", "11111", "00100", "00100", "00000"),
    "=": ("00000", "00000", "11111", "00000", "11111", "00000", "00000"),
}


def png_chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def save_png(path, width, height, rgb):
    rows = bytearray()
    stride = width * 3
    for y in range(height):
        rows.append(0)
        rows.extend(rgb[y * stride:(y + 1) * stride])
    compressed = zlib.compress(bytes(rows), 1 if width * height > 1_000_000 else 6)
    with open(path, "wb") as target:
        target.write(b"\x89PNG\r\n\x1a\n")
        target.write(png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)))
        target.write(png_chunk(b"IDAT", compressed))
        target.write(png_chunk(b"IEND", b""))


def canvas(width, height, color=(255, 255, 255)):
    return bytearray(color * (width * height))


def draw_text(image, width, height, text, scale, jitter=0, color=(20, 28, 38), seed=11):
    rng = random.Random(seed)
    glyph_width = 5 * scale
    glyph_height = 7 * scale
    spacing = scale * 2
    text_width = len(text) * (glyph_width + spacing) - spacing
    x_start = max(0, (width - text_width) // 2)
    y_start = max(0, (height - glyph_height) // 2)
    for index, character in enumerate(text):
        glyph = FONT.get(character)
        if glyph is None:
            continue
        x_offset = x_start + index * (glyph_width + spacing) + (rng.randint(-jitter, jitter) if jitter else 0)
        y_offset = y_start + (rng.randint(-jitter, jitter) if jitter else 0)
        for row, bits in enumerate(glyph):
            for column, bit in enumerate(bits):
                if bit != "1":
                    continue
                for dy in range(scale):
                    for dx in range(scale):
                        x = x_offset + column * scale + dx
                        y = y_offset + row * scale + dy
                        if 0 <= x < width and 0 <= y < height:
                            position = (y * width + x) * 3
                            image[position:position + 3] = bytes(color)


def rotate_clockwise(image, width, height):
    rotated = bytearray(width * height * 3)
    new_width, new_height = height, width
    for y in range(height):
        for x in range(width):
            source = (y * width + x) * 3
            target = (x * new_width + (height - 1 - y)) * 3
            rotated[target:target + 3] = image[source:source + 3]
    return rotated, new_width, new_height


def generate(output_dir):
    os.makedirs(output_dir, exist_ok=True)
    width, height = 1600, 900
    image = canvas(width, height)
    draw_text(image, width, height, "3x - 7 = 11", 14)
    save_png(os.path.join(output_dir, "clear-equation.png"), width, height, image)

    image = canvas(width, height, (250, 248, 238))
    draw_text(image, width, height, "3x - 7 = 11", 12, jitter=3, color=(36, 44, 58), seed=37)
    save_png(os.path.join(output_dir, "handwritten-math.png"), width, height, image)

    image = canvas(width, height, (248, 250, 255))
    draw_text(image, width, height, "H2O + NaCl", 15, color=(12, 42, 105))
    save_png(os.path.join(output_dir, "chemistry-formula.png"), width, height, image)

    image = canvas(width, height)
    draw_text(image, width, height, "3x - 7 = 11", 12)
    image, rotated_width, rotated_height = rotate_clockwise(image, width, height)
    save_png(os.path.join(output_dir, "rotated-equation.png"), rotated_width, rotated_height, image)

    image = canvas(width, height, (220, 220, 220))
    draw_text(image, width, height, "3x - 7 = 11", 5, color=(120, 120, 120))
    save_png(os.path.join(output_dir, "blurry-equation.png"), width, height, image)

    rng = random.Random(9042)
    huge_width, huge_height = 2400, 1600
    noise = bytearray(rng.randbytes(huge_width * huge_height * 3))
    save_png(os.path.join(output_dir, "huge-over-10mb.png"), huge_width, huge_height, noise)

    with open(os.path.join(output_dir, "corrupt.png"), "wb") as target:
        target.write(b"\x89PNG\r\n\x1a\nnot-an-image")
    with open(os.path.join(output_dir, "pdf-renamed-jpg.jpg"), "wb") as target:
        target.write(b"%PDF-1.7\nThis is a PDF, not a JPEG image.\n")

    huge_path = os.path.join(output_dir, "huge-over-10mb.png")
    if os.path.getsize(huge_path) <= 10 * 1024 * 1024:
        raise RuntimeError("The generated huge-over-10mb.png test image is not larger than 10 MB.")


if __name__ == "__main__":
    destination = sys.argv[1] if len(sys.argv) > 1 else ".p09-test-images"
    generate(destination)
