# vue-designer-workbench Specification

## Purpose
Define the Vue designer's unified responsive workbench while preserving editing and integration compatibility.

## Requirements

### Requirement: React-style Vue designer workbench
The Vue designer SHALL provide a compact workbench layout with a single command header, an editing canvas, contextual canvas controls, and a persistent desktop property region while preserving LogicFlow editing behavior.

#### Scenario: Open process design on desktop
- **WHEN** a user enters the process design step on a desktop viewport
- **THEN** the designer shows process identity and commands in one header and keeps the canvas visible beside the node tools and selected-node properties

#### Scenario: Use either process model
- **WHEN** the loaded definition uses classic or mimic mode
- **THEN** the workbench renders and edits that mode without changing its node types or serialized flow JSON

### Requirement: Responsive property editing
The Vue designer SHALL render selected-node properties in an inline right panel on supported desktop viewports and in an overlay panel on narrow viewports.

#### Scenario: Select a node on desktop
- **WHEN** a desktop user selects an editable node or edge
- **THEN** its property form opens beside the canvas and the LogicFlow viewport resizes to the remaining space

#### Scenario: Select a node on mobile
- **WHEN** a user selects an editable node or edge on a narrow viewport
- **THEN** the property form opens as a full-width overlay without permanently reducing the canvas width

### Requirement: Single workbench experience
The Vue designer SHALL use the React-style workbench as its only layout and SHALL NOT expose a legacy-layout selection option.

#### Scenario: Existing host upgrades
- **WHEN** an existing host upgrades the component
- **THEN** it receives the new workbench while existing props, events, slots, commands, data providers, UI adapters, and flow JSON remain compatible

### Requirement: Theme and viewport resilience
The workbench SHALL remain usable in light, dark, desktop, tablet, and mobile contexts without overlapping controls or clipped labels.

#### Scenario: Render dark mode
- **WHEN** the document uses the supported dark theme
- **THEN** the workbench surfaces, borders, controls, canvas, and property region use readable dark-theme tokens

#### Scenario: Resize the viewport
- **WHEN** the viewport crosses desktop and mobile layout thresholds
- **THEN** controls and panels reflow without covering required commands or leaving LogicFlow with stale dimensions
