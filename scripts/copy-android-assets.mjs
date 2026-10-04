import { copyFile } from 'node:fs/promises'
await copyFile(new URL('../public/operator-logo.svg',import.meta.url),new URL('../dist-android/operator-logo.svg',import.meta.url))
