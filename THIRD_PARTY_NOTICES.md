# Third-party notices

## TextResource

`textresource/src/main/java/com/markedusduplicate/textresource/TextResource.kt` is adapted from
[TextResource](https://github.com/dkmarkell/textresource) by Derek Markell, used under the MIT
License. The library is not depended on — its core is one small file, so it is vendored and trimmed:
only the `raw` and `simple` factories are kept (upstream also has `plural`), `resolveString` is
renamed to `asString`, and resolution takes `Resources` rather than `Context`, which is the seam
Compose's own `stringResource()` reads.

```
MIT License

Copyright (c) 2025 Derek Markell

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
