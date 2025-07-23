> [!IMPORTANT]
> This repository contains a *deprecated Kotlin prototype* of our spreadsheet CRDT based on an *operation-based* design. We have since *abandoned this approach* and reimplemented the project in *Scala using a delta-state CRDT*. All further development happens in the [Bismuth monorepo](https://github.com/stg-tud/Bismuth/tree/main/Modules/Tabular) which combines all projects related to distributed systems programming done at STG at TU Darmstadt.
> For now, this repository will stay as is and the old readme is kept below.

# Spreadsheet CRDT

A Replicated Data Type (RDT) for spreadsheets, enabling Google Docs-like collaborative editing with offline synchronization capabilities.

## Motivation
Replicated data types (RDTs) such as CRDTs are becoming increasingly popular as a simplified way for programming distributed systems. RDTs are local data structures with a familiar interface (such as sets, lists, trees) that can automatically synchronize data between multiple devices in the background. For more background on CRDTs, see https://crdt.tech/.

CRDTs for simple data types like sets or lists are well understood, however, modern collaborative applications such as Notion or Google Docs also include more complicated application specific data structures such as tables/spreadsheets. In a spreadsheet, we have certain dependencies between rows and columns and a spreadsheet CRDT algorithm has to decide what happens if multiple devices edit them concurrently and potentially produce conflicts.
