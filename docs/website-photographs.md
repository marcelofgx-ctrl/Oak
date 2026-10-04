# Supplied website photographs

Five user-supplied WhatsApp screenshots were retouched with the image editing tool, removing messenger controls, contact information, black margins and captions. The edit brief called for restrained exposure/white balance/detail correction and preservation of the installation, damage and wear. These are AI-assisted retouched derivatives, not untouched documentary originals; retain the originals separately for inspection or evidence. No original screenshots or contact names are committed to this public repository.

Assets in public/images use descriptive filenames:
- fireplace-room: room and white mantel; hero and gallery.
- fireplace-detail: glowing insert and dark surround; gallery.
- chimney-detail: stone chimney/cap close view; Georgia section and gallery.
- chimney-before: worn cap and ladder; labelled Before.
- chimney-after: roof view after the work; labelled After.

The before/after uses the ordering provided by the source captions and explicitly notes the different camera angles. No project address, customer identity, date, location or technical repair claim is inferred. Supplied photographs replace conceptual illustrations in the visible website. Old SVG assets remain available in the repository for future use.

Each image has 640px and 1280px WebP variants. Total WebP asset size is approximately 1.5MB across all ten variants; browsers select a variant via srcset rather than loading both sizes. Below-fold images use lazy loading, with eager high-priority hero loading. Gallery images can be enlarged in a native dialog with an accessible close control and Escape support. The original edited PNGs remain outside the repository.

Verification: lint, six existing request-model tests, production build and whitespace checks pass. Chromium checks passed for all five gallery photos and their enlarged versions, Escape closing, hero image loading and horizontal overflow at mobile/desktop widths. Screenshots of hero/gallery were inspected. The existing guided request browser checks also passed after the gallery integration.
