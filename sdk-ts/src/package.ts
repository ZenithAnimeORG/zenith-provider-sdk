/**
 * Package envelope specification and archive validation constants and validators.
 *
 * Defines the physical boundaries, size limits, and security rules for portable `.zpk` packages.
 *
 * @module package
 */

import { PluginManifest } from "./models.js";

/**
 * Legacy package format identifier for JVM/Dex bundles.
 */
export const PACKAGE_FORMAT_V1 = "zpk/v1";

/**
 * Portable TypeScript/QuickJS package format identifier.
 */
export const PACKAGE_FORMAT_V2 = "zpk/v2";

/**
 * Current standard package format for all modern Zenith provider packages.
 */
export const CURRENT_PACKAGE_FORMAT = PACKAGE_FORMAT_V2;

/**
 * Current runtime module API version.
 */
export const CURRENT_MODULE_API_VERSION = "1.0.0";

/**
 * Maximum compressed size of a `.zpk` archive in bytes (16 MiB).
 */
export const MAX_PACKAGE_SIZE_BYTES = 16 * 1024 * 1024; // 16 MiB

/**
 * Maximum size of the bundled JavaScript entrypoint file in bytes (5 MiB).
 */
export const MAX_ENTRYPOINT_BYTES = 5 * 1024 * 1024; // 5 MiB

/**
 * Maximum size of `manifest.json` in bytes (256 KiB).
 */
export const MAX_MANIFEST_BYTES = 256 * 1024; // 256 KiB

/**
 * Maximum size of `settings.json` in bytes (128 KiB).
 */
export const MAX_SETTINGS_BYTES = 128 * 1024; // 128 KiB

/**
 * Maximum size of `icon.png` in bytes (256 KiB).
 */
export const MAX_ICON_BYTES = 256 * 1024; // 256 KiB

/**
 * Maximum size of any individual static resource file in bytes (2 MiB).
 */
export const MAX_RESOURCE_BYTES = 2 * 1024 * 1024; // 2 MiB

/**
 * Maximum total uncompressed size across all files in the archive in bytes (32 MiB).
 */
export const MAX_TOTAL_UNCOMPRESSED_BYTES = 32 * 1024 * 1024; // 32 MiB

/**
 * Maximum number of files permitted inside a `.zpk` archive (100 entries).
 */
export const MAX_ENTRY_COUNT = 100;

/**
 * Validation result containing status and error messages.
 */
export interface ValidationResult {
  /**
   * `true` if all validation checks passed without errors.
   */
  readonly valid: boolean;

  /**
   * List of human-readable error messages explaining any detected violations.
   */
  readonly errors: string[];
}

/**
 * Checks whether an entry filename is safe and free of directory traversal vulnerabilities.
 *
 * Rejects absolute paths (`/foo`), path parent traversals (`..`), backslashes, double slashes,
 * and characters outside alphanumeric and `_-./`.
 *
 * @param name File path within the ZIP archive.
 * @returns `true` if the path is safe to extract or inspect, otherwise `false`.
 *
 * @example
 * ```ts
 * isSafeEntryName("manifest.json");       // true
 * isSafeEntryName("icons/icon.png");       // true
 * isSafeEntryName("../secret.txt");        // false (traversal)
 * isSafeEntryName("/etc/passwd");          // false (absolute path)
 * ```
 */
export function isSafeEntryName(name: string): boolean {
  if (!name || name.trim() === "") return false;
  if (name.startsWith("/") || name.startsWith("\\")) return false;
  if (name === "." || name === "..") return false;
  if (name.includes("..")) return false;
  if (name.includes("//") || name.includes("\\\\")) return false;
  const parts = name.split(/[/\\]/);
  if (parts.some((p) => p === "." || p === ".." || p.trim() === "")) return false;
  return /^[a-zA-Z0-9_\-./]+$/.test(name);
}

/**
 * Validates a parsed `manifest.json` against Zenith Manifest V2 rules.
 *
 * Checks:
 * - Root object is non-null
 * - `id` is a valid identifier (1-128 chars `[A-Za-z0-9._-]`)
 * - `name` and `version` are non-empty strings
 * - `packageFormat` is `"zpk/v2"` (or legacy `"zpk/v1"`)
 * - `capabilities` is a non-empty array
 *
 * @param manifest Raw parsed JSON object.
 * @returns {@link ValidationResult} with validation status and error list.
 *
 * @example
 * ```ts
 * const result = validateManifest({
 *   id: "my_provider",
 *   name: "My Provider",
 *   version: "1.0.0",
 *   capabilities: ["CATALOG"]
 * });
 * console.log(result.valid); // true
 * ```
 */
export function validateManifest(manifest: any): ValidationResult {
  const errors: string[] = [];

  if (!manifest || typeof manifest !== "object") {
    return { valid: false, errors: ["Manifest must be a non-null object"] };
  }

  if (typeof manifest.id !== "string" || !/^[A-Za-z0-9._-]{1,128}$/.test(manifest.id)) {
    errors.push("Manifest 'id' must be 1-128 alphanumeric characters (including . _ -)");
  }

  if (typeof manifest.name !== "string" || manifest.name.trim() === "") {
    errors.push("Manifest 'name' must be a non-empty string");
  }

  if (typeof manifest.version !== "string" || manifest.version.trim() === "") {
    errors.push("Manifest 'version' must be a non-empty string");
  }

  if (manifest.packageFormat && manifest.packageFormat !== PACKAGE_FORMAT_V1 && manifest.packageFormat !== PACKAGE_FORMAT_V2) {
    errors.push(`Unsupported packageFormat: '${manifest.packageFormat}'. Expected '${PACKAGE_FORMAT_V2}'`);
  }

  if (!Array.isArray(manifest.capabilities) || manifest.capabilities.length === 0) {
    errors.push("Manifest 'capabilities' must be a non-empty array of ProviderCapability");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

/**
 * Validates that a `.zpk` package envelope satisfies physical archive constraints.
 *
 * Enforces:
 * - Entry count <= 100 entries ({@link MAX_ENTRY_COUNT})
 * - Total uncompressed size <= 32 MiB ({@link MAX_TOTAL_UNCOMPRESSED_BYTES})
 * - Entrypoint size <= 5 MiB ({@link MAX_ENTRYPOINT_BYTES})
 * - `manifest.json` size <= 256 KiB ({@link MAX_MANIFEST_BYTES})
 * - Path traversal security via {@link isSafeEntryName}
 * - No banned native/compiled binaries (`.jar`, `.dex`, `.class`, `.so`, `.dll`, `.dylib`, `.exe`, `.apk`) in `zpk/v2`
 * - Mandatory presence of `manifest.json` and declared entrypoint (e.g. `index.js`)
 *
 * @param entries List of file names and uncompressed sizes in the archive.
 * @param manifest The parsed plugin manifest.
 * @returns {@link ValidationResult} with validation status and error list.
 *
 * @example
 * ```ts
 * const result = validateEnvelope([
 *   { name: "manifest.json", size: 1024 },
 *   { name: "index.js", size: 120000 },
 *   { name: "icon.png", size: 45000 }
 * ], manifest);
 *
 * if (!result.valid) {
 *   console.error("Envelope validation failed:", result.errors);
 * }
 * ```
 */
export function validateEnvelope(
  entries: Array<{ name: string; size: number }>,
  manifest: PluginManifest
): ValidationResult {
  const errors: string[] = [];

  if (entries.length > MAX_ENTRY_COUNT) {
    errors.push(`Archive entry count ${entries.length} exceeds maximum limit of ${MAX_ENTRY_COUNT}`);
  }

  let totalBytes = 0;
  const names = new Set<string>();

  for (const entry of entries) {
    if (names.has(entry.name)) {
      errors.push(`Duplicate entry in package: ${entry.name}`);
    }
    names.add(entry.name);

    if (!isSafeEntryName(entry.name)) {
      errors.push(`Unsafe entry path in package: ${entry.name}`);
    }

    totalBytes += entry.size;

    // Check for banned binary types in portable zpk/v2
    const lower = entry.name.toLowerCase();
    if (
      manifest.packageFormat === PACKAGE_FORMAT_V2 &&
      (lower.endsWith(".jar") ||
        lower.endsWith(".dex") ||
        lower.endsWith(".class") ||
        lower.endsWith(".so") ||
        lower.endsWith(".dll") ||
        lower.endsWith(".dylib") ||
        lower.endsWith(".exe") ||
        lower.endsWith(".apk"))
    ) {
      errors.push(`Forbidden executable binary in portable package: ${entry.name}`);
    }

    if (entry.name === "manifest.json" && entry.size > MAX_MANIFEST_BYTES) {
      errors.push(`manifest.json exceeds size limit of ${MAX_MANIFEST_BYTES} bytes`);
    }

    const entrypoint = manifest.entrypoint ?? "index.js";
    if (entry.name === entrypoint && entry.size > MAX_ENTRYPOINT_BYTES) {
      errors.push(`Entrypoint '${entrypoint}' exceeds size limit of ${MAX_ENTRYPOINT_BYTES} bytes`);
    }
  }

  if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
    errors.push(
      `Total uncompressed bytes ${totalBytes} exceeds maximum limit of ${MAX_TOTAL_UNCOMPRESSED_BYTES}`
    );
  }

  if (!names.has("manifest.json")) {
    errors.push("Package envelope is missing required entry: manifest.json");
  }

  const entrypoint = manifest.entrypoint ?? "index.js";
  if (!names.has(entrypoint)) {
    errors.push(`Package envelope is missing declared entrypoint: ${entrypoint}`);
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}
