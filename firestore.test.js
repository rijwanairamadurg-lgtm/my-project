const test = require('node:test');
const assert = require('node:assert');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');

const PROJECT_ID = 'demo-no-project';
const DATABASE_ID = 'ai-studio-android-calculat-764a6125-eca8-4992-b1e0-ed2d31eaa3ef';

test('Firestore Security Rules', async (t) => {
  let testEnv;

  t.before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8'),
        host: '127.0.0.1',
        port: 8085,
        databaseId: DATABASE_ID
      }
    });
  });

  t.after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  await t.test('Unauthenticated user cannot read or write', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
    const pageRef = unauthedDb.doc('users/alice/notebook_pages/1');

    await assertFails(pageRef.get());
    await assertFails(pageRef.set({
      id: 1,
      title: 'Test',
      content: 'Hello',
      userId: 'alice'
    }));
  });

  await t.test('Authenticated user can read and write own pages', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore({ databaseId: DATABASE_ID });
    const pageRef = aliceDb.doc('users/alice/notebook_pages/1');

    await assertSucceeds(pageRef.set({
      id: 1,
      title: 'Alice Notes',
      content: 'My confidential notes',
      userId: 'alice'
    }));

    await assertSucceeds(pageRef.get());
  });

  await t.test('Authenticated user cannot read or write other users pages', async () => {
    const bobDb = testEnv.authenticatedContext('bob').firestore({ databaseId: DATABASE_ID });
    const pageRef = bobDb.doc('users/alice/notebook_pages/1');

    await assertFails(pageRef.get());
    await assertFails(pageRef.set({
      id: 1,
      title: 'Hacked',
      content: 'Hacked notes',
      userId: 'bob'
    }));
  });
});
