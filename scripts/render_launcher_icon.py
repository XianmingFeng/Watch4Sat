#!/usr/bin/env python3
"""Render Play Store and QA previews from the approved launcher icon geometry."""

from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw
from PIL.PngImagePlugin import PngInfo


BACKGROUND = "#FCFCFF"
CYAN = "#4285F4"
YELLOW = "#FBBC04"

LAYER_SIZE = 108.0
VISIBLE_ORIGIN = 18.0
VISIBLE_SIZE = 72.0

CENTER = (54.0, 54.0)
RING_RADIUS = 28.0
RING_WIDTH = 9.0
RING_START_DEGREES = 1.685
RING_SWEEP_DEGREES = 270.0
POINTER_HALF_WIDTH = 14.0
POINTER_NOTCH_DEPTH = 6.5

POINTER = (
    (65.315490, 43.331110),
    (49.871231278, 77.134460634),
    (44.996382256, 62.489125301),
    (30.662870954, 56.761957261),
)
DOT_CENTER = (71.5, 37.5)
DOT_RADIUS = 8.5

MAX_ARTWORK_RADIUS = math.dist(CENTER, DOT_CENTER) + DOT_RADIUS
PLAY_KEYLINE_FRACTION = 0.75
PLAY_VISIBLE_SIZE = 2.0 * MAX_ARTWORK_RADIUS / PLAY_KEYLINE_FRACTION
PLAY_VISIBLE_ORIGIN = (LAYER_SIZE - PLAY_VISIBLE_SIZE) / 2.0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--store-output", type=Path, required=True)
    parser.add_argument("--qa-dir", type=Path)
    return parser.parse_args()


def validate_geometry() -> None:
    axis_x = DOT_CENTER[0] - CENTER[0]
    axis_y = DOT_CENTER[1] - CENTER[1]
    center_to_dot = math.hypot(axis_x, axis_y)
    unit_axis = (axis_x / center_to_dot, axis_y / center_to_dot)
    unit_normal = (-unit_axis[1], unit_axis[0])

    def local(point: tuple[float, float]) -> tuple[float, float]:
        relative = (point[0] - CENTER[0], point[1] - CENTER[1])
        return (
            relative[0] * unit_axis[0] + relative[1] * unit_axis[1],
            relative[0] * unit_normal[0] + relative[1] * unit_normal[1],
        )

    tip, first_wing, notch, second_wing = map(local, POINTER)
    if abs(tip[1]) > 0.002 or abs(notch[1]) > 0.002:
        raise ValueError("Pointer tip and notch must remain on the satellite axis")
    if not math.isclose(first_wing[0], second_wing[0], abs_tol=0.002):
        raise ValueError("Pointer wings moved to different positions on the axis")
    if not math.isclose(first_wing[1], -second_wing[1], abs_tol=0.002):
        raise ValueError("Pointer wings are no longer mirror images")
    if not math.isclose(first_wing[1], POINTER_HALF_WIDTH, abs_tol=0.000002):
        raise ValueError("Pointer width changed")
    if not math.isclose(
        notch[0] - first_wing[0],
        POINTER_NOTCH_DEPTH,
        abs_tol=0.000002,
    ):
        raise ValueError("Pointer notch depth changed")
    if not first_wing[0] < notch[0] < tip[0]:
        raise ValueError("Pointer vertices are out of order")

    inner_ring_radius = RING_RADIUS - RING_WIDTH / 2.0
    for name, point in (("first", POINTER[1]), ("second", POINTER[3])):
        if not math.isclose(
            math.dist(CENTER, point),
            inner_ring_radius,
            abs_tol=0.000002,
        ):
            raise ValueError(f"Pointer {name} wing no longer touches the inner ring")
        angle = math.degrees(
            math.atan2(point[1] - CENTER[1], point[0] - CENTER[0])
        ) % 360.0
        if (angle - RING_START_DEGREES) % 360.0 > RING_SWEEP_DEGREES:
            raise ValueError(f"Pointer {name} wing moved outside the visible ring arc")

    if any(
        math.dist(CENTER, point) >= inner_ring_radius
        for point in (POINTER[0], POINTER[2])
    ):
        raise ValueError("Pointer tip or notch reaches the ring")

    gap = center_to_dot - DOT_RADIUS - tip[0]
    if not math.isclose(gap, 0.0, abs_tol=0.000002):
        raise ValueError(f"Pointer-to-dot contact changed: {gap:.9f}")

    pointer_radius = max(math.dist(CENTER, point) for point in POINTER)
    max_radius = max(
        RING_RADIUS + RING_WIDTH / 2.0,
        MAX_ARTWORK_RADIUS,
        pointer_radius,
    )
    if max_radius > 33.0:
        raise ValueError(f"Artwork exceeds the 66dp safe circle: {max_radius:.6f}")


def render(
    size: int,
    viewport_origin: float = VISIBLE_ORIGIN,
    viewport_size: float = VISIBLE_SIZE,
    supersample: int = 8,
) -> Image.Image:
    scale = size * supersample / viewport_size

    def point(x: float, y: float) -> tuple[float, float]:
        return ((x - viewport_origin) * scale, (y - viewport_origin) * scale)

    canvas_size = size * supersample
    image = Image.new("RGBA", (canvas_size, canvas_size), BACKGROUND)
    draw = ImageDraw.Draw(image)

    cx, cy = point(*CENTER)
    radius = RING_RADIUS * scale
    ring_end = RING_START_DEGREES + RING_SWEEP_DEGREES
    outer_radius = radius + RING_WIDTH * scale / 2.0
    inner_radius = radius - RING_WIDTH * scale / 2.0
    outer_points = []
    inner_points = []
    for step in range(361):
        angle_degrees = RING_START_DEGREES + RING_SWEEP_DEGREES * step / 360
        angle = math.radians(angle_degrees)
        outer_points.append(
            (
                cx + outer_radius * math.cos(angle),
                cy + outer_radius * math.sin(angle),
            )
        )
        inner_points.append(
            (
                cx + inner_radius * math.cos(angle),
                cy + inner_radius * math.sin(angle),
            )
        )
    draw.polygon(outer_points + list(reversed(inner_points)), fill=CYAN)

    cap_radius = RING_WIDTH * scale / 2.0
    for angle_degrees in (RING_START_DEGREES, ring_end):
        angle = math.radians(angle_degrees)
        cap_x = cx + radius * math.cos(angle)
        cap_y = cy + radius * math.sin(angle)
        draw.ellipse(
            (
                cap_x - cap_radius,
                cap_y - cap_radius,
                cap_x + cap_radius,
                cap_y + cap_radius,
            ),
            fill=CYAN,
        )

    draw.polygon([point(*vertex) for vertex in POINTER], fill=CYAN)

    dot_x, dot_y = point(*DOT_CENTER)
    dot_radius = DOT_RADIUS * scale
    draw.ellipse(
        (
            dot_x - dot_radius,
            dot_y - dot_radius,
            dot_x + dot_radius,
            dot_y + dot_radius,
        ),
        fill=YELLOW,
    )

    return image.resize((size, size), Image.Resampling.LANCZOS)


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    png_info = PngInfo()
    png_info.add(b"sRGB", b"\x00")
    image.save(path, format="PNG", optimize=True, pnginfo=png_info)


def circular_preview(image: Image.Image) -> Image.Image:
    size = image.width
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    preview = Image.new("RGB", (size, size), "#E7EAEE")
    preview.paste(image, mask=mask)
    return preview


def main() -> None:
    args = parse_args()
    validate_geometry()

    store_icon = render(
        512,
        viewport_origin=PLAY_VISIBLE_ORIGIN,
        viewport_size=PLAY_VISIBLE_SIZE,
    )
    save_png(store_icon, args.store_output)

    if args.qa_dir:
        save_png(render(24), args.qa_dir / "watch4sat-icon-24.png")
        save_png(render(48), args.qa_dir / "watch4sat-icon-48.png")
        save_png(
            circular_preview(render(512)),
            args.qa_dir / "watch4sat-icon-circle-512.png",
        )


if __name__ == "__main__":
    main()
