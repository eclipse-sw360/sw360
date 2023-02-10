import React, {useState} from 'react';
import './App.css';
import RouterConfig from './routing/route.configuration';
import PageHeader from './views/utils/header/header';
import PageFooter from './views/utils/footer/footer';

const App = () => {
  return (
    <>
      <PageHeader />
      <RouterConfig />
      <PageFooter />
    </>
  );
}

export default App;