"""Package the reproducible project without local SDKs, caches, or demo databases."""
from pathlib import Path
import zipfile
import os

root = Path(__file__).resolve().parent.parent
target = root / "artifacts/yellowbite-source.zip"
excluded = {".tools", ".gradle", ".idea", ".venv", "build", "__pycache__", ".pytest_cache", ".git", "artifacts"}
target.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
    for directory, folders, files in os.walk(root):
        folders[:] = [folder for folder in folders if folder not in excluded]
        for name in files:
            path = Path(directory) / name
            relative = path.relative_to(root)
            if name != "local.properties" and path.suffix not in {".db", ".db-shm", ".db-wal"}:
                archive.write(path, "yellowbite/" + relative.as_posix())
    for folder in ['screenshots', 'test-results']:
        for path in (root / 'artifacts' / folder).rglob('*'):
            if path.is_file():
                archive.write(path, 'yellowbite/' + path.relative_to(root).as_posix())
print(target)
