package com.theneuron.pricer.services;

import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;

public interface MoneyExchanger {
    Money exchange(Money from, CurrencyUnit to) throws Exception;
}
