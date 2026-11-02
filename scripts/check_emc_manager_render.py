"""Guard the compiled EMC screen against drawing a second background over its labels.

Run gradlew build first. This checks extract-call ordering, not visual game output.
Minecraft 26.3 replaced Screen#render with extractRenderState/extractBackground, so the
panel fills must stay in extractBackground and the labels must follow the base extract pass.
"""

from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SCREEN = "moze_intel.projecte.gameObjs.gui.EMCManagerScreen"
EXTRACTOR = "GuiGraphicsExtractor"


def method_body(bytecode, name):
    match = re.search(
        rf"(?ms)^  public void {name}\([^\n]*\);\r?\n(.*?)(?=^  (?:public|protected|private)|^}})",
        bytecode,
    )
    assert match, f"Missing compiled method: {name}"
    return match.group(1)


def main():
    result = subprocess.run(
        ["javap", "-c", "-p", "-classpath", str(ROOT / "build/classes/java/main"), SCREEN],
        check=True,
        capture_output=True,
        text=True,
        # Only ASCII method names are inspected; javap's string constants use the local console encoding.
        encoding="ascii",
        errors="replace",
    )
    state = method_body(result.stdout, "extractRenderState")
    background = method_body(result.stdout, "extractBackground")
    assert state.count("Screen.extractRenderState:") == 1, "Extract the base widgets exactly once"
    assert "Screen.extractBackground:" not in state, "Do not re-extract the background inside extractRenderState()"
    assert f"{EXTRACTOR}.fill:" not in state, "Panel fills belong in extractBackground()"
    assert state.index("Screen.extractRenderState:") < state.index(f"{EXTRACTOR}."), "Labels must follow the base extract pass"
    assert background.count("Screen.extractBackground:") == 1, "Draw the vanilla background once"
    assert f"{EXTRACTOR}.fill:" in background, "The panel background must be filled in extractBackground()"
    assert background.index("Screen.extractBackground:") < background.index(f"{EXTRACTOR}.fill:"), "Panel must follow the vanilla background"
    print("PASS: compiled rendering order is base extract -> panel fill, then widgets and labels after the base extract")


if __name__ == "__main__":
    main()
