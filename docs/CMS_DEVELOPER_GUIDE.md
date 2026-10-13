# CMS Developer Guide

## Architecture Overview

The CMS is built on top of the existing framework's patterns:
- **Backend:** Spring Boot with JPA entities, a dedicated `CmsController` for specialized operations, and the existing `DynamicCrudController` for standard CRUD
- **Frontend:** React + MUI with `SiteSettingsContext` for global state, reusable CMS components, and admin pages

## Database Schema

### site_setting
Key-value store for site-wide configuration.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-increment |
| setting_key | VARCHAR(255) | UNIQUE, NOT NULL |
| setting_value | VARCHAR(4000) | |
| setting_type | VARCHAR(50) | `text`, `image`, `number` |
| created_at, updated_at | TIMESTAMP | Audit |
| created_by, updated_by | VARCHAR(255) | Audit |

### cms_template
Defines page layout structures using a JSON schema.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-increment |
| name | VARCHAR(255) | NOT NULL |
| description | VARCHAR(1000) | |
| structure | TEXT | JSON schema (see below) |
| is_active | BOOLEAN | Default: true |
| Audit fields | | created_at/by, updated_at/by |

### cms_page
Individual content pages referencing a template.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-increment |
| title | VARCHAR(255) | NOT NULL |
| slug | VARCHAR(255) | UNIQUE, NOT NULL |
| template_id | BIGINT | FK to cms_template |
| content | TEXT | JSON content values |
| status | VARCHAR(50) | `draft`, `published`, `archived` |
| published_at | TIMESTAMP | Set on publish |
| meta_title | VARCHAR(255) | SEO |
| meta_description | VARCHAR(500) | SEO |
| Audit fields | | created_at/by, updated_at/by |

### cms_file
Registry of uploaded files.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-increment |
| original_name | VARCHAR(255) | Original filename |
| stored_name | VARCHAR(255) | UUID-based filename |
| file_path | VARCHAR(500) | e.g., `/uploads/2026/04/uuid.png` |
| mime_type | VARCHAR(100) | |
| file_size | BIGINT | Bytes |
| created_at | TIMESTAMP | |
| created_by | VARCHAR(255) | |

## Template Structure JSON Spec

Templates use a JSON schema stored in the `structure` column:

```json
{
  "sections": [
    {
      "key": "hero",              // Unique identifier (used as content key prefix)
      "label": "Hero Section",    // Display label in the page editor
      "blocks": [
        {
          "key": "heading",       // Unique within the section
          "type": "text",         // Block type (see below)
          "label": "Heading",     // Display label
          "required": true        // Whether field is required
        }
      ]
    }
  ]
}
```

### Block Types

| Type | Editor Widget | Renderer Output |
|------|--------------|-----------------|
| `text` | TextField (single-line) | Typography |
| `richtext` | TextField (multiline, HTML) | HTML via dangerouslySetInnerHTML |
| `image` | ImageUpload component | img tag |
| `html` | TextField (multiline, monospace) | HTML via dangerouslySetInnerHTML |

### Page Content JSON

Content values are stored in a flat JSON object keyed by `{sectionKey}.{blockKey}`:

```json
{
  "hero.heading": "Welcome",
  "hero.background_image": "/uploads/2026/04/hero.jpg",
  "body.content": "<p>Main content here</p>"
}
```

## How to Add a Custom Block Type

1. **Backend** — No changes needed. Block types are a frontend concern; the backend stores and serves content as-is.

2. **Frontend — Template Builder** (`TemplateStructureEditor.jsx`):
   Add your type to the `BLOCK_TYPES` array:
   ```js
   const BLOCK_TYPES = ['text', 'richtext', 'image', 'html', 'video'];
   ```

3. **Frontend — Page Content Editor** (`PageContentEditor.jsx`):
   Add a case in the `renderBlockInput` switch:
   ```jsx
   case 'video':
     return (
       <TextField
         key={key} fullWidth label={block.label}
         value={value}
         onChange={(e) => handleBlockChange(section.key, block.key, e.target.value)}
         placeholder="YouTube URL"
         sx={{ mb: 2 }}
       />
     );
   ```

4. **Frontend — Page Renderer** (`PageRenderer.jsx`):
   Add a case in the `renderBlock` switch:
   ```jsx
   case 'video':
     return (
       <Box key={block.key} sx={{ mb: 2 }}>
         <iframe src={value} width="100%" height="400" frameBorder="0" allowFullScreen />
       </Box>
     );
   ```

## Key Backend Files

| File | Purpose |
|------|---------|
| `model/SiteSetting.java` | Site setting entity |
| `model/CmsTemplate.java` | Template entity |
| `model/CmsPage.java` | Page entity |
| `model/CmsFile.java` | File upload entity |
| `repository/SiteSettingRepository.java` | Settings data access |
| `repository/CmsTemplateRepository.java` | Template data access |
| `repository/CmsPageRepository.java` | Page data access |
| `repository/CmsFileRepository.java` | File data access |
| `controller/CmsController.java` | CMS REST endpoints |
| `service/CmsService.java` | Settings, rendering, publishing, reordering |
| `service/FileStorageService.java` | File upload/delete operations |
| `config/WebConfig.java` | Static file serving for uploads |

## Key Frontend Files

| File | Purpose |
|------|---------|
| `context/SiteSettingsContext.js` | Global settings state |
| `components/cms/ImageUpload.jsx` | Reusable image upload component |
| `components/cms/TemplateStructureEditor.jsx` | Template section/block editor |
| `components/cms/PageContentEditor.jsx` | Dynamic form from template structure |
| `components/cms/PageRenderer.jsx` | Public page display |
| `components/cms/MenuTreeEditor.jsx` | Hierarchical menu editor |
| `pages/admin/cms/SiteSettingsPage.jsx` | Settings admin page |
| `pages/admin/cms/MenuManagerPage.jsx` | Menu admin page |
| `pages/admin/cms/TemplatesListPage.jsx` | Template list |
| `pages/admin/cms/TemplateBuilderPage.jsx` | Template create/edit |
| `pages/admin/cms/PagesListPage.jsx` | Page list |
| `pages/admin/cms/PageEditorPage.jsx` | Page create/edit |

## Configuration

Upload settings in `application.yml`:

```yaml
app:
  upload:
    dir: ./uploads           # File storage directory
    max-size: 10MB           # Maximum upload size
    allowed-types: image/png,image/jpeg,image/gif,image/svg+xml,image/webp
```

The upload directory is created automatically on first upload.

## Security

- All CMS admin endpoints require authentication
- `PUT/POST/DELETE` operations on CMS controller require `Admin` role via `@PreAuthorize`
- Uploaded files and rendered pages are publicly accessible
- File uploads are validated for MIME type before storage
- Table names go through `DynamicCrudService.validateTable()` for SQL injection prevention
