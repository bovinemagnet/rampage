id: __SCENARIO_ID__
name: __SCENARIO_ID__
protocol: graphql
endpointRef: graphql
# TODO: set the GraphQL operationName to match your query file.
operationName: TODO

description: >
  TODO: describe what this scenario exercises.

headers:
  X-Scenario-Id: __SCENARIO_ID__
  X-Business-Process: TODO

request:
  graphqlQueryFile: config/graphql/__SCENARIO_ID__.graphql
  variables:
    # TODO: bind one variable per declared feeder column.
    # exampleVar: ${feeder:exampleColumn}

feeder:
  type: jdbc
  databaseRef: sourceData
  sqlFile: config/queries/__SCENARIO_ID__-data.sql
  strategy: circular
  preload: true
  failIfEmpty: false
  columns:
    # TODO: declare each column returned by the SQL query.
    # exampleColumn:
    #   type: string
    #   required: true
    #   sessionKey: exampleVar

checks:
  httpStatus: 200
  jsonPath:
    - path: $.errors
      expectation: absentOrEmpty
    # TODO: add at least one assertion about the response payload.

workload:
  inheritFromRun: true

tags:
  - graphql
  - TODO

safety:
  mutating: false
  idempotent: true
