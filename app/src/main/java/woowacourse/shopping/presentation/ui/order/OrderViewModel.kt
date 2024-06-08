package woowacourse.shopping.presentation.ui.order

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import woowacourse.shopping.data.repository.CouponRepositoryImpl
import woowacourse.shopping.data.repository.OrderRepositoryImpl
import woowacourse.shopping.domain.repository.CouponRepository
import woowacourse.shopping.domain.repository.OrderRepository
import woowacourse.shopping.presentation.ui.UiState
import woowacourse.shopping.presentation.ui.model.CouponModel
import woowacourse.shopping.presentation.ui.model.toUiModel
import woowacourse.shopping.presentation.util.Event
import woowacourse.shopping.remote.api.ApiClient
import woowacourse.shopping.remote.api.CouponApiService
import woowacourse.shopping.remote.datasource.RemoteCartDataSourceImpl
import woowacourse.shopping.remote.datasource.RemoteCouponDataSourceImpl
import woowacourse.shopping.remote.datasource.RemoteOrderDataSourceImpl

class OrderViewModel(
    private val couponRepository: CouponRepository,
    private val orderRepository: OrderRepository,
    private val selectedCartIds: List<Long>,
    val totalPriceWithoutDiscount: Long,
) : ViewModel() {
    private val _coupons: MutableLiveData<UiState<List<CouponModel>>> = MutableLiveData()
    val coupons: LiveData<UiState<List<CouponModel>>> = _coupons

    private val _discountAmount: MutableLiveData<Int> = MutableLiveData(0)
    val discountAmount: LiveData<Int> = _discountAmount

    val totalPriceWithDiscount: LiveData<Long> =
        discountAmount.map { shippingFee.value?.plus(totalPriceWithoutDiscount)?.minus(it) ?: 0 }

    private val couponsData get() = (coupons.value as? UiState.Success)?.data ?: emptyList()

    private val _shippingFee: MutableLiveData<Int> = MutableLiveData(OrderRepository.SHIPPING_FEE)
    val shippingFee: LiveData<Int> = _shippingFee

    private val _completeOrder: MutableLiveData<Event<Boolean>> = MutableLiveData()
    val completeOrder: LiveData<Event<Boolean>> = _completeOrder

    init {
        viewModelScope.launch {
            couponRepository.findCoupons(selectedCartIds)
                .onSuccess { result ->
                    val couponModels = result.map { it.toUiModel() }
                    _coupons.value = UiState.Success(couponModels)
                }
                .onFailure {
                }
        }
    }

    companion object {
        class Factory(
            private val selectedCartIds: List<Long>,
            private val totalPriceWithoutDiscount: Long,
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val couponApiService = ApiClient.getApiClient().create(CouponApiService::class.java)
                return OrderViewModel(
                    CouponRepositoryImpl(
                        RemoteCouponDataSourceImpl(couponApiService),
                        RemoteCartDataSourceImpl(),
                    ),
                    OrderRepositoryImpl(RemoteOrderDataSourceImpl()),
                    selectedCartIds,
                    totalPriceWithoutDiscount,
                ) as T
            }
        }
    }
}