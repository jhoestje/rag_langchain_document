// seed.js
db = db.getSiblingDB("nachadb");
db.paymentBatchStates.insertMany([
  { state: "PENDING" },
  { state: "PROCESSING" },
  { state: "COMPLETE" },
  { state: "CANCELED" },
  { state: "FAILED" }
]);
db.accounts.insertMany([
  { accountId: "emp2", accountNumber: '11', accountName: 'Sally Reynolds', amount: 100 },
  { accountId: "emp1", accountNumber: '21', accountName: 'Bob Smith', amount: 100},
  { accountId: "emp3", accountNumber: '31', accountName: 'Joe Edwards', amount: 100},
  { accountId: "emp4", accountNumber: '41', accountName: 'Jim Reynolds', amount: 100},
  { accountId: "com4", accountNumber: '1234', accountName: 'Bob\'s Manufacturing', amount: 100000},
]);