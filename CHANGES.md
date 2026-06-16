# Webin-CLI Change Log

## 9.0.3 (unreleased)

### New Features

#### GFF3 Annotation Support for Genome Submissions (TTENA-207)

Genome submissions can now include a GFF3 annotation file alongside the
sequence FASTA file. This enables client-side validation of GFF3 annotation
against the submitted sequences before upload.

**New manifest field:**

```
GFF3    myAnnotation.gff3.gz
```

**New supported file-group combinations:**

- `FASTA` + `GFF3` — GFF3-annotated genome assembly
- `FASTA` + `GFF3` + `CHROMOSOME_LIST` — Chromosomal GFF3-annotated assembly
- `FASTA` + `GFF3` + `CHROMOSOME_LIST` + `UNLOCALISED_LIST` — Chromosomal GFF3-annotated assembly with unlocalised sequences

**Accepted GFF3 file suffixes (must be compressed):**

- `.gff3.gz`, `.gff.gz`
- `.gff3.bz2`, `.gff.bz2`

**Validation:**

GFF3 files are validated client-side using the `gff3tools` library.
Validation includes:

- GFF3 format and syntax checks
- Sequence-feature cross-validation (location, translation, gap checks)
  against the companion FASTA sequences

Validation messages are written to the per-file `.report` file in the
validation directory.

**Constraints:**

- GFF3 must be paired with a `FASTA` file. Combining GFF3 with `FLATFILE`
  is not supported.
- GFF3 is not permitted for `primary metagenome`, `binned metagenome`, or
  `clinical isolate assembly` types.

**Analysis XML:**

The generated analysis XML includes a `<FILE filetype="gff3"/>` element for
the GFF3 file alongside the FASTA entry.

> **Coordination note:** The ENA analysis-file XSD must be updated to accept
> `filetype="gff3"` before submitting to the production service. Until then,
> restrict GFF3 genome submissions to the test service (`-test` flag).

---

## 9.0.2

- MAG submission is not possible if sample has a metagenome taxonomy.
- Minor logging improvements.

## 9.0.1

- New platform support (ENA-6751).

## 9.0.0

- Updated to Java 17.
- Spring Boot 2.7.5.
